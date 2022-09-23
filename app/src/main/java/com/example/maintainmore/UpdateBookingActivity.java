package com.example.maintainmore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.splashscreen.SplashScreen;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class UpdateBookingActivity extends AppCompatActivity {

    private static final String TAG = "UpdateBookingActivityInfo";
    private final int cancelTillHour = 1;

    Toolbar toolbar;

    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;
    FirebaseFirestore db;
    Calendar calendar;

    String userID;


    Button buttonCancel, buttonUpdate;
    Button buttonChooseDate, buttonChooseTime;

    TextView displayServiceName, displayServiceDescription, displayTotalItems, displayTotalPrice;
    TextView displayRequiredTime, displayServicePrice;
    TextView displayServiceDate, displayServiceTime;
    ImageView imageBackground;

    int numberOfServicesForMale = 0;
    int numberOfServicesForFemale = 0;
    int servicePrice = 0;
    int totalServices = 0;
    int totalServicesPrice = 0;
    int totalWalletBalance = 0;


    String chosenDate= "";
    String chosenTime = "";
    String cancellationTime = "";
    String bookingDate = "";
    String bookingTime = "";

    String bookingID;
    String serviceName, serviceDescription, servicesForMale, servicesForFemale, serviceRequiredTime;
    String visitingDate, visitingTime, totalServiceItems,servicePricePerItem, totalServicePrice;

    String paymentDate, paymentTime;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen.installSplashScreen(this);
        setContentView(R.layout.activity_update_booking);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        userID = Objects.requireNonNull(firebaseUser).getUid();

        db = FirebaseFirestore.getInstance();
        calendar = Calendar.getInstance();

        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);


        GetUserInfo();
        LinkedViewIDes();
        GetDataFromBookingCard();
        SetDataToView();

        ServicePickerForMale();
        ServicePickerForFemale();


        buttonChooseDate.setOnClickListener(view -> DatePickerForService());
        buttonChooseTime.setOnClickListener(view -> TimePickerForService());

        buttonCancel.setOnClickListener(view -> CancelBooking());
        buttonUpdate.setOnClickListener(view -> UpdateBooking());

        bookingDate = DateFormat.getDateInstance().format(calendar.getTime());
        bookingTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
    }



    @SuppressLint("LongLogTag")
    private void CancelBooking() {

        db.collection("Bookings").document(bookingID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    db.collection("Bookings Cancelled").document(bookingID)
                            .set(Objects.requireNonNull(document.getData()));
                    Log.d(TAG, "DocumentSnapshot data: " + document.getString("assignedTechnician"));

                    db.collection("Bookings Cancelled").document(bookingID)
                            .update("bookingStatus", "Cancelled").addOnCompleteListener(task1 ->
                            Log.d(TAG,"Cancel updated" ));

                    db.collection("Technicians")
                            .document(Objects.requireNonNull(document.getString("assignedTechnician")))
                            .update("availabilityStatus","Free").addOnCompleteListener(task1 ->
                            Log.d(TAG, "update")).addOnFailureListener(e -> Log.d(TAG, "ere: " + e.getMessage()));

                    db.collection("Bookings").document(bookingID).delete().addOnSuccessListener(unused -> {
                        Log.i(TAG, "BookingCanceled");
                        finish();
                    });

                    totalWalletBalance += totalServicesPrice;

                    Map<String, String> paymentTransaction = new HashMap<>();

                    paymentDate = DateFormat.getDateInstance().format(calendar.getTime());
                    paymentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

                    paymentTransaction.put("bookingID",bookingID);
                    paymentTransaction.put("whoPaidAmount", userID);
                    paymentTransaction.put("paymentDate",paymentDate);
                    paymentTransaction.put("paymentTime",paymentTime);
                    paymentTransaction.put("amountPaid",String.valueOf(totalServicesPrice));
                    paymentTransaction.put("paymentStatus","Cr");



                    db.collection("Payments").document().set(paymentTransaction).addOnSuccessListener(unused ->
                            Log.i(TAG, "Refund Credited in your Wallet")
                    );

                    db.collection("Users").document(userID).update("walletBalanceInINR", String.valueOf(totalWalletBalance))
                            .addOnSuccessListener(unused -> Toast.makeText(this, "Refund Credited in your Wallet", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Log.e(TAG,"Failed to update balance" + e));

                    Log.i(TAG, "Updated Balance: " + totalWalletBalance);

                } else {
                    Log.d(TAG, "No such document");
                }
            } else {
                Log.d(TAG, "get failed with ", task.getException());
            }
        });
    }

    private void UpdateBooking() {
        DocumentReference documentReference = db.collection("Bookings").document(bookingID);

        if (totalServices <= 0){
            Toast.makeText(getApplicationContext(), "Please Select at least 1 Service", Toast.LENGTH_SHORT).show();
            return;
        }
        if (chosenDate.equals("")){
            Toast.makeText(getApplicationContext(), "Please Choose service Date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (chosenTime.equals("")){
            Toast.makeText(getApplicationContext(), "Please Choose service Time", Toast.LENGTH_SHORT).show();
            return;
        }


        documentReference.update(
                "servicesForMale", String.valueOf(numberOfServicesForMale),
                "servicesForFemale", String.valueOf(numberOfServicesForFemale),
                "totalServicesPrice", String.valueOf(totalServicesPrice),
                "totalServices", String.valueOf(totalServices),
                "servicePrice", String.valueOf(servicePrice),
                "visitingDate", chosenDate,
                "bookingDate", bookingDate,
                "cancelTillHour", cancellationTime,
                "visitingTime",chosenTime,
                "bookingTime",bookingTime
        ).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Booking Updates", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e ->
            Toast.makeText(this, "Booking Failed: " + e, Toast.LENGTH_SHORT).show()
        );
    }

    @SuppressLint("SimpleDateFormat")
    private void TimePickerForService() {

        MaterialTimePicker.Builder builder = new MaterialTimePicker.Builder();
        builder.setTitleText("Choose Time");
        builder.setHour(12);
        builder.setMinute(0);

        MaterialTimePicker timePicker = builder.build();

        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
        timePicker.addOnPositiveButtonClickListener(view ->{

            String timePicked = timePicker.getHour()+ ":" + timePicker.getMinute();

            int cancelTime = timePicker.getHour() - cancelTillHour;

            String cancelTimePicked = cancelTime + ":" + timePicker.getMinute();

            SimpleDateFormat f24Hour = new SimpleDateFormat("HH:mm");
            try {
                Date date = f24Hour.parse(timePicked);
                Date cancelDate = f24Hour.parse(cancelTimePicked);
                assert date != null;
                assert cancelDate != null;

                SimpleDateFormat f12Hour = new SimpleDateFormat("hh:mm aa");

                chosenTime = f12Hour.format(date);
                cancellationTime = f12Hour.format(cancelDate);

                Toast.makeText(this, cancellationTime, Toast.LENGTH_SHORT).show();
                displayServiceTime.setText(chosenTime);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
    }

    private void DatePickerForService() {

        long today = MaterialDatePicker.todayInUtcMilliseconds();

        CalendarConstraints.Builder constraintBuilder = new CalendarConstraints.Builder();
        constraintBuilder.setValidator(DateValidatorPointForward.now());

//        Remove <Long> if error
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText("Choose Date");
        builder.setSelection(today);
        builder.setCalendarConstraints(constraintBuilder.build());

//        Remove <Long> if error
        MaterialDatePicker<Long> materialDatePicker = builder.build();
        materialDatePicker.show(getSupportFragmentManager(),"DATE_PICKET");
        materialDatePicker.addOnPositiveButtonClickListener(selection -> {
            chosenDate = String.valueOf(materialDatePicker.getHeaderText());
            displayServiceDate.setText(chosenDate);
        });


    }

    private void ServicePickerForFemale(){
        TextView numberPickerForFemale;
        ImageView buttonDecrementForFemale, buttonIncrementForFemale;

        numberPickerForFemale = findViewById(R.id.numberPickerForFemale);

        numberPickerForFemale.setText(String.valueOf(numberOfServicesForFemale));


        buttonDecrementForFemale = findViewById(R.id.buttonDecrementForFemale);
        buttonIncrementForFemale = findViewById(R.id.buttonIncrementForFemale);

        buttonDecrementForFemale.setOnClickListener(view -> {
            if (numberOfServicesForFemale > 0) {
                numberOfServicesForFemale--;
                totalServices--;
                numberPickerForFemale.setText(String.valueOf(numberOfServicesForFemale));
                displayTotalItems.setText(String.valueOf(totalServices));

                totalServicesPrice = servicePrice * totalServices;
                displayTotalPrice.setText(String.valueOf(totalServicesPrice));
            }
        });

        buttonIncrementForFemale.setOnClickListener(view -> {
            if (numberOfServicesForFemale < 5) {
                numberOfServicesForFemale++;
                totalServices++;

                numberPickerForFemale.setText(String.valueOf(numberOfServicesForFemale));
                displayTotalItems.setText(String.valueOf(totalServices));

                totalServicesPrice = servicePrice * totalServices;
                displayTotalPrice.setText(String.valueOf(totalServicesPrice));
            }
            else {
                Toast.makeText(this, "You Can't Choose More then 5", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void ServicePickerForMale(){
        TextView numberPickerForMale;
        ImageView buttonDecrement, buttonIncrement;

        numberPickerForMale = findViewById(R.id.numberPicker);

        numberPickerForMale.setText(String.valueOf(numberOfServicesForMale));


        buttonDecrement = findViewById(R.id.buttonDecrement);
        buttonIncrement = findViewById(R.id.buttonIncrement);

        buttonDecrement.setOnClickListener(view -> {
            if (numberOfServicesForMale > 0) {
                numberOfServicesForMale--;
                totalServices--;
                numberPickerForMale.setText(String.valueOf(numberOfServicesForMale));
                displayTotalItems.setText(String.valueOf(totalServices));

                totalServicesPrice = servicePrice * totalServices;
                displayTotalPrice.setText(String.valueOf(totalServicesPrice));
            }
        });

        buttonIncrement.setOnClickListener(view -> {
            if (numberOfServicesForMale < 5) {
                numberOfServicesForMale++;
                totalServices++;
                numberPickerForMale.setText(String.valueOf(numberOfServicesForMale));
                displayTotalItems.setText(String.valueOf(totalServices));

                totalServicesPrice = servicePrice * totalServices;
                displayTotalPrice.setText(String.valueOf(totalServicesPrice));
            }
            else {
                Toast.makeText(this, "You Can't Choose More then 5", Toast.LENGTH_SHORT).show();
            }
        });


    }


    private void SetDataToView() {

        displayServiceName.setText(serviceName);
        displayServiceDescription.setText(serviceDescription);
        displayServiceDescription.setText(serviceDescription);
        displayRequiredTime.setText(serviceRequiredTime);
        displayServicePrice.setText(servicePricePerItem);
        displayServiceDate.setText(visitingDate);
        displayServiceTime.setText(visitingTime);
        displayTotalItems.setText(totalServiceItems);
        displayTotalPrice.setText(totalServicePrice);
    }

    @SuppressLint("LongLogTag")
    private void GetUserInfo() {
        db.collection("Users").document(userID)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                            Log.d(TAG, "Balance: " + document.getString("walletBalanceInINR"));

                            totalWalletBalance = Integer.parseInt(Objects.requireNonNull(document.getString("walletBalanceInINR")));
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                });
    }

    @SuppressLint("LongLogTag")
    private void GetDataFromBookingCard(){
        Intent intent = getIntent();

        bookingID = intent.getStringExtra("bookingID");
        serviceName = intent.getStringExtra("serviceName");
        serviceDescription = intent.getStringExtra("serviceDescription");
        servicesForMale = intent.getStringExtra("servicesForMale");
        servicesForFemale = intent.getStringExtra("servicesForFemale");
        serviceRequiredTime = intent.getStringExtra("serviceRequiredTime");
        servicePricePerItem = intent.getStringExtra("servicePrice");
        visitingDate = intent.getStringExtra("visitingDate");
        visitingTime = intent.getStringExtra("visitingTime");
        totalServiceItems = intent.getStringExtra("totalServices");
        totalServicePrice = intent.getStringExtra("totalServicesPrice");

        cancellationTime = intent.getStringExtra("serviceCancellationTime");

        servicePrice = Integer.parseInt(servicePricePerItem);
        if (servicesForMale == null) {
            numberOfServicesForMale = 0;
        }
        else {
            numberOfServicesForMale = Integer.parseInt(servicesForMale);
        }
        if (servicesForFemale == null) {
            numberOfServicesForFemale = 0;
        }
        else {
            numberOfServicesForFemale = Integer.parseInt(servicesForFemale);
        }
        totalServices = Integer.parseInt(totalServiceItems);
        totalServicesPrice = Integer.parseInt(totalServicePrice);
        chosenDate = visitingDate;
        chosenTime = visitingTime;
//        cancellationTime =


        Log.i(TAG,"Name: " + serviceName);
        Log.i(TAG,"Description: " + serviceDescription);
        Log.i(TAG,"Required time: " + serviceRequiredTime);
        Log.i(TAG,"Price: " + servicePricePerItem);

    }

    private void LinkedViewIDes() {
        imageBackground = findViewById(R.id.imageBackground);

        buttonCancel = findViewById(R.id.buttonCancel);
        buttonUpdate = findViewById(R.id.buttonUpdate);
        buttonChooseDate = findViewById(R.id.buttonChooseDate);
        buttonChooseTime = findViewById(R.id.buttonChooseTime);

        displayServiceName = findViewById(R.id.displayServiceName);
        displayServiceDescription = findViewById(R.id.displayServiceDescription);
        displayRequiredTime = findViewById(R.id.displayRequiredTime);
        displayServicePrice = findViewById(R.id. displayServicePrice);

        displayServiceDate = findViewById(R.id.displayServiceDate);
        displayServiceTime = findViewById(R.id.displayServiceTime);

        displayTotalItems = findViewById(R.id.displayTotalItems);
        displayTotalPrice = findViewById(R.id.displayTotalPrice);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}