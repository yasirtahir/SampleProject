package com.sampleproject.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.sampleproject.R;
import com.sampleproject.api.GoEuro;
import com.sampleproject.api.WebServiceFactory;
import com.sampleproject.models.Places;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.android.view.OnClickEvent;
import rx.android.view.ViewObservable;
import rx.android.widget.OnTextChangeEvent;
import rx.android.widget.WidgetObservable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func3;

public class HomeFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DatePickerDialog.OnDateSetListener {

    private AutoCompleteTextView edtFrom;
    private AutoCompleteTextView edtTo;
    private Button btnSearch;
    private TextView txtDate;
    Observable<OnTextChangeEvent> observableTo;
    Observable<OnTextChangeEvent> observableFrom;
    Observable<OnTextChangeEvent> observableDate;
    GoEuro goEuro;
    private ProgressDialog mProgressDialog;
    Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private ArrayAdapter<String> fromArrayAdapter;
    private ArrayAdapter<String> toArrayAdapter;
    private DatePickerDialog datePickerDialog;
    Calendar calendar = Calendar.getInstance();
    Action1<Throwable> onError = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            throwable.printStackTrace();
        }
    };
    Action0 onComplete = new Action0() {
        @Override
        public void call() {
            Log.i("OnComplete", "OnComplete");
        }
    };

    public HomeFragment() {
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setMessage("Getting Location");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        buildGoogleApiClient();

        // Connecting UI Elements
        edtTo = (AutoCompleteTextView) view.findViewById(R.id.edtTo);
        edtFrom = (AutoCompleteTextView) view.findViewById(R.id.edtFrom);
        btnSearch = (Button) view.findViewById(R.id.btnSearch);
        txtDate = (TextView) view.findViewById(R.id.txtDate);
        TextView btnReadMe = (TextView) view.findViewById(R.id.btnReadMe);

        // Initializing Objects/Elements
        datePickerDialog = DatePickerDialog.newInstance(this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), true);
        fromArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<String>());
        toArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<String>());

        // Setting Listeners
        edtFrom.setThreshold(3);
        edtFrom.setAdapter(fromArrayAdapter);
        edtTo.setThreshold(3);
        edtTo.setAdapter(toArrayAdapter);

        txtDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickerDialog.setVibrate(true);
                datePickerDialog.setYearRange(1985, 2028);
                datePickerDialog.setCloseOnSingleTapDay(true);
                datePickerDialog.show(getActivity().getSupportFragmentManager(), DatePickerDialog.class.getSimpleName());
            }
        });

        btnReadMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://github.com/goeuro/mobile-test/blob/master/README.md";
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(i);
            }
        });

        // REST API Client
        goEuro = WebServiceFactory.getInstanceWithBasicGSonConversion();

        observableTo = WidgetObservable.text(edtTo);
        observableFrom = WidgetObservable.text(edtFrom);
        observableDate = WidgetObservable.text(txtDate);
        setUpSubscriptions();
    }

    public void setUpSubscriptions() {
        observableFrom
                .compose(applyApiOperators())
                .subscribe(new Action1<ArrayList<Places>>() {
                    @Override
                    public void call(ArrayList<Places> placeses) {
                        sortPlaces(placeses);
                        ArrayList<String> justTheNames = new ArrayList<>();
                        for (Places place : placeses) {
                            justTheNames.add(place.getFullName());
                        }
                        fromArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, justTheNames);
                        edtFrom.setAdapter(fromArrayAdapter);
                        fromArrayAdapter.notifyDataSetChanged();

                    }
                }, onError, onComplete);

        observableTo
                .compose(applyApiOperators())
                .subscribe(new Action1<ArrayList<Places>>() {
                    @Override
                    public void call(ArrayList<Places> placeses) {
                        sortPlaces(placeses);
                        ArrayList<String> justTheNames = new ArrayList<>();
                        for (Places place : placeses) {
                            justTheNames.add(place.getFullName());
                        }
                        toArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, justTheNames);
                        edtTo.setAdapter(toArrayAdapter);
                        toArrayAdapter.notifyDataSetChanged();
                    }
                }, onError, onComplete);

        Observable.combineLatest(observableFrom, observableTo, observableDate, new Func3<OnTextChangeEvent, OnTextChangeEvent, OnTextChangeEvent, Boolean>() {
            @Override
            public Boolean call(OnTextChangeEvent fromText, OnTextChangeEvent toText, OnTextChangeEvent dateText) {
                return !fromText.text().toString().isEmpty() && !toText.text().toString().isEmpty() && !dateText.text().toString().isEmpty();
            }
        })
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean textFieldsAreFilled) {
                        btnSearch.setEnabled(textFieldsAreFilled);
                    }
                });

        ViewObservable.clicks(btnSearch).subscribe(new Action1<OnClickEvent>() {
            @Override
            public void call(OnClickEvent onClickEvent) {
                Toast.makeText(getActivity(), "Search is not yet implemented", Toast.LENGTH_SHORT).show();
            }
        });
    }

    Observable.Transformer<OnTextChangeEvent, ArrayList<Places>> applyApiOperators() {
        return new Observable.Transformer<OnTextChangeEvent, ArrayList<Places>>() {
            @Override
            public Observable<ArrayList<Places>> call(Observable<OnTextChangeEvent> observable) {
                return observable.debounce(250, TimeUnit.MILLISECONDS)
                        .map(new Func1<OnTextChangeEvent, String>() {
                            @Override
                            public String call(OnTextChangeEvent onTextChangeEvent) {
                                return onTextChangeEvent.text().toString().trim();
                            }
                        })
                        .filter(new Func1<String, Boolean>() {
                            @Override
                            public Boolean call(String s) {
                                return !s.isEmpty() && s.length() > 2;
                            }
                        })
                        .flatMap(new Func1<String, Observable<ArrayList<Places>>>() {
                            @Override
                            public Observable<ArrayList<Places>> call(String s) {
                                // Fetching results in English
                                return goEuro.suggest("en", s);
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }

    private void sortPlaces(ArrayList<Places> placeses) {
        Collections.sort(placeses, new Comparator<Places>() {
            @Override
            public int compare(Places p1, Places p2) {
                final int BEFORE = -1;
                final int EQUAL = 0;
                final int AFTER = 1;
                LatLng currentLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                if (p1.getDistance(currentLocation) < p2.getDistance(currentLocation))
                    return BEFORE;
                else if (p1.getDistance(currentLocation) > p2.getDistance(currentLocation))
                    return AFTER;
                else
                    return EQUAL;
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        mProgressDialog.dismiss();
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mProgressDialog.dismiss();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mProgressDialog.dismiss();
    }

    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
        String formattedDate = String.format(Locale.getDefault(), "%d.%d.%d", day, month, year);
        txtDate.setText(formattedDate);
    }
}