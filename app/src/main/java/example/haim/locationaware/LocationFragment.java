package example.haim.locationaware;


import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import butterknife.Unbinder;


/**
 * Find the user Location Without using the map:
 * //Where is the user
 * //Distance in meters from another location
 * //bearing to another location
 * //Speed
 * //location-> address
 * //address->location
 */
public class LocationFragment extends Fragment {

    @BindView(R.id.tvLocation)
    TextView tvLocation;
    Unbinder unbinder;
    //Api provider
    FusedLocationProviderClient client;
    @BindView(R.id.etAddress)
    EditText etAddress;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_location, container, false);
        unbinder = ButterKnife.bind(this, view);

        client = new FusedLocationProviderClient(getContext());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onResume() {
        super.onResume();
        getLocationUpdates();
    }

    //Might be null especially in the emulator:
    private void getLastKnownLocation() {
        if (!checkLocationPermission()) return;

        Task<Location> task = client.getLastLocation();

        task.addOnCompleteListener(getActivity(), new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    if (location != null) {
                        tvLocation.setText(location.toString());
                        tvLocation.setTextSize(20);
                    }
                }
            }
        });
    }

    private boolean checkLocationPermission() {
        //if no permission -> request it and get out.
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    getActivity()/*Activity*/,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1
            );
            return false;
        }
        return true;
    }

    private void getLocationUpdates() {
        if (!checkLocationPermission()) return;
        LocationRequest request = new LocationRequest();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//GPS
        /*request.setPriority(LocationRequest.PRIORITY_LOW_POWER);//Cellular
        request.setPriority(LocationRequest.PRIORITY_NO_POWER);//Last Known Location
        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);//GPS + Cellular */
        request.setInterval(30 * 1000); //check the gps chip each 30 seconds
        request.setFastestInterval(500);
        //request.setNumUpdates //infinite updates
        //request.setExpirationDuration(60*60*1000);//stop after one hour
        //request.setSmallestDisplacement(100); 100m... //Overrides the interval

        //Executors.newCachedThreadPool()
        //client.requestLocationUpdates()

        client.requestLocationUpdates(request, callback /*callback*/, null/*Looper*/);
    }

    LocationCallback callback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location l = locationResult.getLastLocation();

            String result = String.format(
                    Locale.getDefault(),
                    "(%e, %e)\n Speed: %e\n Time: %d\n",
                    l.getLatitude(), l.getLongitude(), l.getSpeed(), l.getTime());

            tvLocation.setText(result);
            getAddress(l);
            //GeoCoding vs Reverse Geocoding:


        }
    };

    private void getAddress(Location l) {
        Geocoder geocoder = new Geocoder(getContext());
        try {
            List<Address> addresses = geocoder.getFromLocation(l.getLatitude(), l.getLongitude(), 1);
            if (addresses.size() == 0) return; //no result found

            //address is like an array of address lines.
            Address address = addresses.get(0);
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                String line = address.getAddressLine(i);
                Toast.makeText(getContext(), line, Toast.LENGTH_SHORT).show();
                //line:
                //0 -> Street
                //1 -> City
                //2 -> Country
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    //gps->address
    //address->coordinates


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            getLocationUpdates();

        boolean shouldShowRequestPermissionRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(
                        getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION);
    }

    /*
    * Keyboard Search clicked for etAddress - IME Option (see xml file: inputType=text, imeOptions=actionSearch)
    * On IME Option Clicked:
    * android:inputType="text"
    * android:imeOptions="actionSearch"
    */
    @OnEditorAction(R.id.etAddress)
    public boolean searchClicked(TextView text){
        if (text.getText() != null && text.getText().length() > 0) {
            //Toast.makeText(getContext(), text.getText().toString(), Toast.LENGTH_SHORT).show();
            getLocation(text.getText().toString());
        }
        return false;
    }

    //Forward geoCoder
    //get location from address (Edit text -> geo coder):
    public void getLocation(String address){
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());

        try {
            List<Address> addressList = geocoder.getFromLocationName(address, 1);
            if (addressList.size() == 0)return; //no results.

            Address a = addressList.get(0);
            double latitude = a.getLatitude();
            double longitude = a.getLongitude();
            etAddress.setText("(" + latitude + ", "+ longitude + ")");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
