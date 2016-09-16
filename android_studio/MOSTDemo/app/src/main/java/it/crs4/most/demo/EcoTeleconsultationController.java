package it.crs4.most.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import it.crs4.most.demo.eco.AREcoTeleconsultationActivity;
import it.crs4.most.demo.eco.BaseEcoTeleconsultationActivity;
import it.crs4.most.demo.eco.EcoTeleconsultationActivity;
import it.crs4.most.demo.models.Teleconsultation;
import it.crs4.most.demo.setup_fragments.PatientSelectionFragment;
import it.crs4.most.demo.setup_fragments.SetupFragment;
import it.crs4.most.demo.setup_fragments.SummaryFragment;

public class EcoTeleconsultationController extends TeleconsultationController {
    @Override
    public SetupFragment[] getFragments(TeleconsultationSetup teleconsultationSetup) {
        return new SetupFragment[] {
            PatientSelectionFragment.newInstance(teleconsultationSetup),
            SummaryFragment.newInstance(teleconsultationSetup)
        };
    }

    @Override
    public void startTeleconsultationActivity(Activity activity, Teleconsultation teleconsultation) {
        Intent i;
        if (Build.MANUFACTURER.equals("EPSON") && Build.MODEL.equals("embt2")) {
            i = new Intent(activity, AREcoTeleconsultationActivity.class);
        }
        else {
            i = new Intent(activity, EcoTeleconsultationActivity.class);
        }
        i.putExtra(BaseEcoTeleconsultationActivity.TELECONSULTATION_ARG, teleconsultation);
        activity.startActivityForResult(i, EcoTeleconsultationActivity.TELECONSULT_ENDED_REQUEST);
    }
}
