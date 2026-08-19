package com.program.braintrainer

import android.app.Application
import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.program.braintrainer.chess.model.data.BillingClientManager
import com.program.braintrainer.chess.model.data.SettingsManager
import com.program.braintrainer.gamification.AchievementManager
import com.program.braintrainer.stats.AttemptRepository
import com.program.braintrainer.stats.StatsDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application klasa koja drži zavisnosti sa životnim vekom cele aplikacije.
 *
 * Ključno: [BillingClientManager] je ovde, a ne u ViewModel-u, da bi se premium status
 * proveravao pri svakom pokretanju aplikacije - a ne tek kada korisnik otvori Podešavanja.
 * Bez toga refundirana ili povučena kupovina nikada ne bi bila detektovana, a premium
 * prenet backup-om na drugi uređaj nikada ne bi bio poništen.
 */
class BrainTrainerApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsManager: SettingsManager by lazy { SettingsManager(this) }

    /**
     * Jedna instanca za celu aplikaciju. Ranije ju je svaki factory pravio za
     * sebe, pa je `newlyUnlockedAchievementFlow` bio per-instanca i niko nije
     * mogao da čuje otključano dostignuće.
     */
    val achievementManager: AchievementManager by lazy {
        AchievementManager(this, settingsManager)
    }

    /**
     * Istorija odigranih zagonetki. Room dozvoljava jednu instancu baze po
     * procesu, pa i ona živi ovde.
     */
    val attemptRepository: AttemptRepository by lazy {
        AttemptRepository(StatsDatabase.create(this).attemptDao())
    }

    val billingClientManager: BillingClientManager by lazy {
        BillingClientManager(
            context = this,
            externalScope = applicationScope,
            onEntitlementResolved = { hasPremium ->
                applicationScope.launch {
                    // Upisuje se samo stvarna promena; DataStore.edit uvek piše na disk.
                    if (settingsManager.storedPremiumFlow.first() != hasPremium) {
                        settingsManager.setPremiumUser(hasPremium)
                    }
                }
            }
        )
    }

    override fun onCreate() {
        super.onCreate()

        // Padovi iz debug build-a ne treba da zagađuju Crashlytics konzolu.
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // Pristup lazy vrednosti pokreće povezivanje na Google Play i proveru kupovina.
        billingClientManager
    }
}

/** Prečica do [BrainTrainerApp] iz bilo kog konteksta. */
val Context.brainTrainerApp: BrainTrainerApp
    get() = applicationContext as BrainTrainerApp
