package com.example.maintainmore.Fragments;


import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.maintainmore.EditProfileActivity;
import com.example.maintainmore.LoginActivity;
import com.example.maintainmore.ManageAddressActivity;
import com.example.maintainmore.R;


import com.example.maintainmore.SettingsActivity;
import com.example.maintainmore.WalletActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;



import java.util.Objects;

import cn.pedant.SweetAlert.SweetAlertDialog;


public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragmentInfo";

    FirebaseStorage firebaseStorage;
    StorageReference storageReference;

    TextView displayName, displayEmail;
    ImageView profilePicture;
    ListView listViewProfile;
    CardView profileCard;

    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    FirebaseFirestore db ;

    DocumentReference documentReference;


    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        Log.i(TAG,"ProfileFragment");

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        listViewProfile = view.findViewById(R.id.listViewProfile);
        profilePicture = view.findViewById(R.id.profilePicture);

        profileCard = view.findViewById(R.id.profileCard);

        displayName = view.findViewById(R.id.displayName);
        displayEmail = view.findViewById(R.id.displayEmail);


        CurrentUser();

        return view;
    }

    private void CurrentUser() {
        if (firebaseUser!=null) {

            String userID = Objects.requireNonNull(firebaseUser).getUid();

            documentReference = db.collection("Users").document(userID);

            documentReference.addSnapshotListener((value, error) -> {
                if (error != null) {
                    Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
                }
                if (value != null && value.exists()){
                    displayName.setText(value.getString("name"));
                    displayEmail.setText(value.getString("email"));
                    Glide.with(requireActivity()).load(value.getString("imageUrl"))
                            .placeholder(R.drawable.ic_person).into(profilePicture);
                }
            });

            profileCard.setOnClickListener(view1 -> startActivity(new Intent(getActivity(), EditProfileActivity.class)));

            ProfileList();
        }
    }

    private void ProfileList() {

        String[] cities = {"Manage Address", "My Wallet"
                , "Settings", "Delete Account", "Sign Out"};


        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, cities);
        listViewProfile.setAdapter(adapter);

        listViewProfile.setOnItemClickListener((adapterView, view12, i, l) ->
        {

            switch (i){
                case 0:
                    Log.i(TAG, "Manage Address");
                    ManageAddress();
                    break;
                case 1:
                    Log.i(TAG, "My Wallet");
                    MyWallet();
                    break;
                case 2:
                    Log.i(TAG, "Settings");
                    Settings();
                    break;
                case 3:
                    Log.i(TAG, "Delete Account");
                    DeleteAccount();
                    break;
                case 4:
                    Log.i(TAG, "Sign Out");
                    SignOut();
                    break;
            }

        });

    }

    private void SignOut(){

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(R.string.are_you_sure);
        builder.setMessage(R.string.do_you_want_to_sign_out);
        builder.setPositiveButton("Sign Out", (dialogInterface, i) ->{

            firebaseAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            requireActivity().finish();

            Toast.makeText(getActivity(), "Sign out successful", Toast.LENGTH_SHORT).show();

        });
        builder.setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    private void DeleteAccount(){
        String userID = Objects.requireNonNull(firebaseUser).getUid();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setIcon(R.drawable.ic_delete_forever);
        builder.setTitle(R.string.delete_account);
        builder.setMessage(R.string.delete_account_massage);
        builder.setPositiveButton("Yes", (dialogInterface, i) ->
                firebaseUser.delete().addOnCompleteListener(task -> {
                    db.collection("Users").document(userID).delete()
                            .addOnSuccessListener(unused -> Log.i(TAG, "ServiceDeleted"));
                    if (task.isSuccessful()){

                        SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(requireActivity(),SweetAlertDialog.SUCCESS_TYPE);
                        sweetAlertDialog.setTitleText("Account Deleted Successful");
                        sweetAlertDialog.setConfirmClickListener(sweetAlertDialog1->{
                            startActivity(new Intent(requireActivity(),LoginActivity.class));
                            requireActivity().finishAffinity();
                            sweetAlertDialog1.dismissWithAnimation();
                        }).setCanceledOnTouchOutside(false);
                        sweetAlertDialog.show();
                    }
                    else {
                        new SweetAlertDialog(requireActivity(),SweetAlertDialog.ERROR_TYPE)
                                .setTitleText(Objects.requireNonNull(task.getException()).getMessage()).show();
                    }
                }));
        builder.setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss());

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void Settings(){
        startActivity(new Intent(getActivity(), SettingsActivity.class));
    }


    private void MyWallet() {
        startActivity(new Intent(getActivity(), WalletActivity.class));
    }

    private void ManageAddress() {
        startActivity(new Intent(getActivity(), ManageAddressActivity.class));
    }

}