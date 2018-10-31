package com.prasimax.applicationexample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

public class InputFunctionDialog extends DialogFragment {
    String HINT1_KEY = "HINT1",
            HINT2_KEY = "HINT2",
            TITLE_KEY = "TITLE",
            N2_ACTIVE_KEY = "NUMBER2_ACTIVE",
            SENDER_ID_KEY = "SENDER";

    TextView hint1, hint2, hintHex;
    EditText editNumber1, editNumber2, editHex;
    int number1, number2; String hexData;

    public InputFunctionDialogInterface getListener() {
        return listener;
    }

    public void setListener(InputFunctionDialogInterface listener) {
        this.listener = listener;
    }

    InputFunctionDialogInterface listener;

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    Bundle bundle;

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    boolean test = false;

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    boolean asString = false;

    public boolean isAsString() {
        return asString;
    }

    public void setAsString(boolean set){
        this.asString = set;
    }

    boolean custom = false;


    public interface InputFunctionDialogInterface {
        void onDialogExit(DialogFragment dialog, boolean isAccepted);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        if(isTest()){
            return onCreateDialog2(savedInstanceState);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Generate View Content
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.input_function_dialog, null);

        String temp = bundle.getString(TITLE_KEY);
        builder.setTitle(temp==null? TITLE_KEY: temp);

        hint1 = view.findViewById(R.id.numberHint1);
        editNumber1 = view.findViewById(R.id.editNumber1);
        hint2 = view.findViewById(R.id.numberHint2);
        editNumber2 = view.findViewById(R.id.editNumber2);

        if(isCustom()){
            hint1.setVisibility(View.GONE);
            hint2.setVisibility(View.GONE);
            editNumber1.setVisibility(View.GONE);
            editNumber2.setVisibility(View.GONE);

            if(isAsString()){
                hintHex = view.findViewById(R.id.textHint);
                editHex = view.findViewById(R.id.editText);
                editHex.setHint("Set your String here.");
            }else {
                hintHex = view.findViewById(R.id.hexHint);
                editHex = view.findViewById(R.id.editHex);
                editHex.setHint("Set your Hex here.");
            }

            hintHex.setVisibility(View.VISIBLE);
            editHex.setVisibility(View.VISIBLE);

            temp = bundle.getString(HINT1_KEY);
            hintHex.setText(temp == null ? HINT1_KEY : temp);


            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listener.onDialogExit(InputFunctionDialog.this, false);
                }
            }).setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    InputFunctionDialog.this.hexData = InputFunctionDialog.this.editHex.getText().toString();
                    listener.onDialogExit(InputFunctionDialog.this, true);
                }
            });

        }else {

            temp = bundle.getString(HINT1_KEY);
            hint1.setText(temp == null ? HINT1_KEY : temp);

            editNumber1.setHint("Set your: " + (temp == null ? HINT1_KEY : temp) + " here.");

            if (!bundle.getBoolean(N2_ACTIVE_KEY)) {
                hint2.setVisibility(View.GONE);
                editNumber2.setVisibility(View.GONE);
            } else {
                temp = bundle.getString(HINT2_KEY);
                hint2.setText(temp == null ? HINT2_KEY : temp);
                editNumber2.setHint("Set your: " + (temp == null ? HINT2_KEY : temp) + " here.");
            }

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listener.onDialogExit(InputFunctionDialog.this, false);
                }
            }).setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String input = InputFunctionDialog.this.editNumber1.getText().toString();
                    number1 = Integer.parseInt(input);
                    if(bundle.getBoolean(N2_ACTIVE_KEY)){
                        input = InputFunctionDialog.this.editNumber2.getText().toString();
                        number2 = Integer.parseInt(input);
                    }
                    listener.onDialogExit(InputFunctionDialog.this, true);
                }
            });
        }

        builder.setView(view);
        // End of -> Generate View Content

        return builder.create();
    }

    NumberPicker np;
    public Dialog onCreateDialog2(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        String temp = bundle.getString(TITLE_KEY);
        builder.setTitle(temp==null? TITLE_KEY: temp);

        //temp = bundle.getString(HINT1_KEY);

        np = new NumberPicker(getContext());
        np.setMinValue(1);
        np.setMaxValue(10);
        builder.setView(np);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onDialogExit(InputFunctionDialog.this, false);
            }
        }).setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                number1 = np.getValue();
                listener.onDialogExit(InputFunctionDialog.this, true);
            }
        });

        return builder.create();
    }

    public static InputFunctionDialog createNewDefaultDialog(InputFunctionDialogInterface listener, MenuItem item, String HINT1, String HINT2, boolean enableSecondInput ){
        InputFunctionDialog dialog = new InputFunctionDialog();
        dialog.setListener(listener);
        Bundle bundle = new Bundle();
        bundle.putString(dialog.TITLE_KEY, (String) item.getTitle());
        bundle.putInt(dialog.SENDER_ID_KEY, item.getItemId());
        bundle.putString(dialog.HINT1_KEY, HINT1);
        if(enableSecondInput){
            bundle.putBoolean(dialog.N2_ACTIVE_KEY, enableSecondInput);
            bundle.putString(dialog.HINT2_KEY, HINT2);
        }
        dialog.setBundle(bundle);
        return dialog;
    }
}
