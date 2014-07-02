package com.inspiredandroid.linuxcommandbibliotheca.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.inspiredandroid.linuxcommandbibliotheca.CommandBibliothecaActivity;
import com.inspiredandroid.linuxcommandbibliotheca.R;
import com.inspiredandroid.linuxcommandbibliotheca.adapter.CommandsExpandableListAdapter;
import com.inspiredandroid.linuxcommandbibliotheca.models.CommandModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by simon on 13.06.14.
 */
public class BibliothecaFragment extends Fragment implements View.OnClickListener {

    CommandsExpandableListAdapter adapter;
    ArrayList<ArrayList<CommandModel>> childs = new ArrayList<ArrayList<CommandModel>>();
    ArrayList<String> group = new ArrayList<String>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bibliotheca, container, false);

        // command categories
        group.add(getString(R.string.system_info));
        childs.add( new ArrayList<CommandModel>());
        group.add(getString(R.string.system_control));
        childs.add( new ArrayList<CommandModel>());
        group.add("Audio/Video");
        childs.add( new ArrayList<CommandModel>());

        // get raw commands file
        InputStream inputStream = getResources().openRawResource(R.raw.commands);

        // convert file to arraylist
        ArrayList<CommandModel> commandsAll = new Gson().fromJson(readTextFile(inputStream), new TypeToken<List<CommandModel>>(){}.getType());

        // sort commands by category
        for(CommandModel command : commandsAll) {
            if(command.getCategory() == 0) {
                childs.get(0).add(command);
            } else if(command.getCategory() == 1) {
                childs.get(1).add(command);
            } else if(command.getCategory() == 2) {
                childs.get(2).add(command);
            }
        }

        // init list adapter
        adapter = new CommandsExpandableListAdapter(getActivity(), group, childs);


        initViews(view);

        return view;
    }


    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.btnFilter) {
            showFilterDialog();
        }
    }

    ArrayList mSelectedItems;
    private void showFilterDialog() {

        String[] items = new String[]{"Gnome", "KDE"};

        mSelectedItems = new ArrayList();  // Where we track the selected items
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Set the dialog title
        builder.setTitle("Filter")
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                .setMultiChoiceItems(items, null,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    mSelectedItems.add(which);
                                } else if (mSelectedItems.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    mSelectedItems.remove(Integer.valueOf(which));
                                }
                            }
                        }
                )
                        // Set the action buttons
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the mSelectedItems results somewhere
                        // or return them to the component that opened the dialog

                    }
                })
                .setNegativeButton("Reset", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     *
     * @param view
     */
    private void initViews(View view) {

        ExpandableListView list = (ExpandableListView) view.findViewById(R.id.expandableListView);
        list.setAdapter(adapter);
        list.setOnChildClickListener( new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i2, long l) {

                CommandModel commandModel = (CommandModel) view.getTag(R.id.ID);
                Context context = view.getContext();

                String iconBase64 = getBase64StringByResourceName(context, commandModel.getIconResource());
                commandModel.setIconBase64(iconBase64);

                Intent data = new Intent();
                data.putExtra(CommandBibliothecaActivity.EXTRA_COMMAND, commandModel);

                returnResult(data);

                return false;
            }
        });

        EditText etSearch = (EditText) view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener( new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }
            @Override
            public void afterTextChanged(Editable editable) {
                String query = editable.toString();
                if(query.length()>0) {
                    search(query);
                } else {
                    resetSearchResults();
                }
            }
        });

        ImageButton btnFilter = (ImageButton) view.findViewById(R.id.btnFilter);
        btnFilter.setVisibility(View.GONE);
        btnFilter.setOnClickListener(this);
    }

    /**
     *
     * @param context
     * @param resName
     * @return
     */
    private String getBase64StringByResourceName(Context context, String resName) {
        int imageRes = context.getResources().getIdentifier(resName, "drawable", context.getPackageName());

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), imageRes);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);

        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    /**
     * reset adapter entries
     */
    private void resetSearchResults() {
        adapter.updateEntries(group, childs);
    }

    /**
     * search for query in all commands and update adapter
     * @param query
     */
    private void search(String query) {
        ArrayList<ArrayList<CommandModel>> entries = new ArrayList<ArrayList<CommandModel>>();
        for(ArrayList<CommandModel> childGroup : childs) {
            ArrayList<CommandModel> newChildGroup = new ArrayList<CommandModel>();
            for(CommandModel child : childGroup) {
                if(child.getCommand().toLowerCase().contains(query.toLowerCase()) || child.getDesc().toLowerCase().contains(query.toLowerCase())) {
                    newChildGroup.add(child);
                }
            }
            entries.add(newChildGroup);
        }
        adapter.updateEntries(group, entries);
    }

    /**
     *
     */
    private void returnResult(Intent data) {
        Activity activity = getActivity();
        if(activity != null) {
            activity.setResult(Activity.RESULT_OK, data);
            activity.finish();
        }
    }

    /**
     *
     * @param inputStream
     * @return
     */
    public String readTextFile(InputStream inputStream) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {

        }
        return outputStream.toString();
    }

}
