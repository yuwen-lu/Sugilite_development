package edu.cmu.hcii.sugilite.verbal_instruction_demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;

/**
 * @author toby
 * @date 12/10/17
 * @time 4:17 AM
 */
public class OverlayChosenPopupDialog {
    //this dialog should allow the user to confirm the currently selected query, or switch to a different query
    private Context context;
    private AlertDialog dialog;
    private VerbalInstructionOverlayManager overlayManager;

    public OverlayChosenPopupDialog(Context context, LayoutInflater inflater, VerbalInstructionOverlayManager overlayManager, VerbalInstructionResults.VerbalInstructionResult chosenResult, List<VerbalInstructionResults.VerbalInstructionResult> allResults, SerializableUISnapshot serializableUISnapshot){
        this.context = context;
        this.overlayManager = overlayManager;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Const.appNameUpperCase + " Verbal Instruction");


        List<String> operationList = new ArrayList<>();

        //fill in the options
        operationList.add("Confirm parse: " + chosenResult.getFormula());
        operationList.add("Choose a different parse");
        operationList.add("Remove overlays");
        String[] operations = new String[operationList.size()];
        operations = operationList.toArray(operations);
        final String[] operationClone = operations.clone();
        builder.setItems(operationClone, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        //confirm parse
                        //TODO: confirm parse, send the result back to the server
                        Toast.makeText(context, "Confirmed parse: " + chosenResult.getFormula(), Toast.LENGTH_SHORT).show();
                        overlayManager.removeOverlays();
                        break;
                    case 1:
                        //choose a different parse
                        DifferentParseChooseDialog differentParseChooseDialog = new DifferentParseChooseDialog(context, inflater, overlayManager, allResults, serializableUISnapshot);
                        differentParseChooseDialog.show();
                        break;
                    case 2:
                        //remove overlays
                        overlayManager.removeOverlays();
                        break;
                }
            }
        });
        dialog = builder.create();
    }

    public void show(){
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
        dialog.show();
    }
}