package com.opentaxi.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.opentaxi.android.R;
import com.opentaxi.android.TaxiApplication;
import com.opentaxi.models.NewCRequest;
import com.opentaxi.models.NewCRequestDetails;
import com.opentaxi.models.RequestCView;
import com.opentaxi.rest.RestClient;
import com.stil.generated.mysql.tables.pojos.Regions;
import com.taxibulgaria.enums.RegionsType;
import com.taxibulgaria.enums.RequestStatus;
import org.androidannotations.annotations.*;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: stanimir
 * Date: 4/18/13
 * Time: 2:58 PM
 * developer STANIMIR MARINOV
 */
//@WindowFeature(Window.FEATURE_NO_TITLE)
//@Fullscreen
@EFragment(R.layout.requests)
public class RequestsFragment extends Fragment {

    private static final String TAG = "RequestsFragment";
    //private static final int REQUEST_DETAILS = 100;

    //private boolean isFromHistory = false;
    private long lastShowRequests = 0;

    @ViewById
    android.widget.TableLayout requests_table;

    @ViewById
    android.widget.ProgressBar pbProgress;

    Activity mActivity;

    OnCommandListener mListener;

    private boolean history = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity) {
            try {
                mListener = (OnCommandListener) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString() + " must implement OnRequestEventsListener");
            }
            mActivity = (Activity) context;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            history = bundle.getBoolean("history", false);
        }
    }

    @AfterViews
    void afterRequestsActivity() {
        if (requests_table != null) {
            requests_table.setVisibility(View.INVISIBLE);
            pbProgress.setVisibility(View.VISIBLE);
            getRequests();
        } else {
            Log.e(TAG, "requests_table=null");
            //finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        TaxiApplication.requestsPaused();
    }

    @Override
    public void onResume() {
        super.onResume();
        TaxiApplication.requestsResumed();
        scheduleRequestSec();
    }

    /*@OnActivityResult(REQUEST_DETAILS)
    void onResult() {
    }*/

    @Background(delay = 1000)
    void scheduleRequestSec() {
        if (TaxiApplication.isRequestsVisible()) {
            if (history) getRequestHistory();
            else getRequests();
        }
    }

    @Background(delay = 10000)
    void scheduleRequest() {
        if (TaxiApplication.isRequestsVisible()) {
            if (history) getRequestHistory();
            else getRequests();
        }
    }

    @Background
    void getRequests() {
        setActivityTile(getString(R.string.active_requests));
        if (new Date().getTime() > (lastShowRequests + 5000)) {
            RequestCView requestView = new RequestCView();
            requestView.setPage(1);
        /*requestView.setFilters(filters);  todo set pages
        requestView.setRows(rows);

        requestView.setSord(sord);
        requestView.setSidx(sidx);
        requestView.setSearchField(searchField);
        requestView.setSearchString(searchString);
        requestView.setSearchOper(searchOper);*/
            requestView.setMy(true);
            showRequests(RestClient.getInstance().getRegions(RegionsType.BURGAS_STATE.getCode()), RestClient.getInstance().getRequests(requestView));
        }
        scheduleRequest();
    }

    @Background
    void getRequestHistory() {
        setActivityTile(getString(R.string.request_history));
        RequestCView requestView = new RequestCView();
        requestView.setPage(1);
        requestView.setMy(true);
        requestView.setRequestStatus(RequestStatus.NEW_REQUEST_DONE.getCode());
        showRequests(RestClient.getInstance().getRegions(RegionsType.BURGAS_STATE.getCode()), RestClient.getInstance().getRequests(requestView));
    }

    @UiThread
    void setActivityTile(String title) {

        if (mListener != null) mListener.setBarTitle(title);
    }

    @UiThread
    void showRequests(Regions[] regions, RequestCView requests) {
        synchronized (this) {
            long currDate = new Date().getTime();
            if (TaxiApplication.isRequestsVisible() && currDate > (lastShowRequests + 5000)) {
                lastShowRequests = currDate;
                requests_table.setVisibility(View.VISIBLE);
                pbProgress.setVisibility(View.GONE);
                TableRow row;
                if (requests != null && requests.getGridModel() != null) {

                    TextView time, address, car, state;

                    row = new TableRow(mActivity);

            /*id = new TextView(this);
            id.setPadding(2, 2, 2, 2);
            id.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.1f));*/

            /*date = new TextView(this);
            date.setPadding(2, 2, 2, 2);
            date.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.1f));*/

                    address = new TextView(mActivity);
                    address.setPadding(2, 2, 2, 2);
                    address.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.4f));

                    time = new TextView(mActivity);
                    time.setPadding(2, 2, 2, 2);
                    time.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f));

                    car = new TextView(mActivity);
                    car.setPadding(2, 2, 2, 2);
                    car.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f));

                    state = new TextView(mActivity);
                    state.setPadding(2, 2, 2, 2);
                    state.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f));

                    //id.setText("Номер");
                    //date.setText("Дата и час");
                    address.setText(R.string.address);
                    time.setText(R.string.execution_time);
                    car.setText(R.string.car);
                    state.setText(R.string.status);

                    //row.addView(id);
                    //row.addView(date);
                    row.addView(address);
                    row.addView(time);
                    row.addView(car);
                    row.addView(state);

                    if (requests_table != null) {
                        requests_table.removeAllViews();
                        requests_table.addView(row, new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                        for (final NewCRequest newCRequest : requests.getGridModel()) {
                            if (!newCRequest.getStatus().equals(RequestStatus.NEW_REQUEST_MSG.getCode())) {
                                row = new TableRow(mActivity);

               /* id = new TextView(this);
                id.setPadding(2, 2, 2, 2);
                id.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.1f));

                date = new TextView(this);
                date.setPadding(2, 2, 2, 2);
                date.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.1f));*/

                                address = new TextView(mActivity);
                                address.setPadding(2, 2, 2, 2);
                                address.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.4f));

                                time = new TextView(mActivity);
                                time.setPadding(2, 2, 2, 2);
                                time.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f));

                                car = new TextView(mActivity);
                                car.setPadding(2, 2, 2, 2);
                                car.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f));

                                state = new TextView(mActivity);
                                state.setPadding(2, 2, 2, 2);
                                state.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f));

                                //id.setText(requestMap.get("id").toString());
                                //date.setText(requestMap.get("datecreated").toString()); //DateFormat.getDateInstance(DateFormat.SHORT).format(requestMap.get("datecreated")));
                                //Regions regions = RestClient.getInstance().getRegionById(RegionsType.BURGAS_STATE.getCode(), newCRequest.getRegionId());
                                Regions region = getRegionById(regions, newCRequest.getRegionId());
                                if (region != null) {
                                    address.setText(region.getDescription() + " " + newCRequest.getFullAddress());
                                } else address.setText(newCRequest.getFullAddress());
                                time.setText(newCRequest.getDispTime() + " мин.");
                                if (newCRequest.getCarNumber() != null && !newCRequest.getCarNumber().equals(""))
                                    car.setText(" №" + newCRequest.getCarNumber()); //newCRequest.getNotes() todo display taxi company

                                if (newCRequest.getStatus() != null) {
                                    String statusCode = RequestStatus.getByCode(newCRequest.getStatus()).toString();
                                    int resourceID = getResources().getIdentifier(statusCode, "string", mActivity.getPackageName());
                                    if (resourceID > 0) {
                                        try {
                                            state.setText(resourceID);
                                        } catch (Resources.NotFoundException e) {
                                            Log.e(TAG, "Resources.NotFoundException:" + resourceID);
                                        }
                                    } else state.setText(statusCode);
                                }

                                row.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        if (mListener != null) mListener.startRequestDetails(newCRequest);
                                        /*Intent requestsIntent = new Intent(RequestsFragment.this, RequestDetailsActivity_.class);
                                        requestsIntent.putExtra("newCRequest", toRequestDetails(newCRequest));
                                        RequestsFragment.this.startActivityForResult(requestsIntent, REQUEST_DETAILS);*/
                                    }
                                });
                   /* row.setBackgroundColor(getResources().getColor(R.color.red_color));*/


                                //row.addView(id);
                                //row.addView(date);
                                row.addView(address);
                                row.addView(time);
                                row.addView(car);
                                row.addView(state);

                                requests_table.addView(row, new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            }
                        }
                    }

                } else {
                    Log.i(TAG, "requests=null");
                    row = new TableRow(mActivity);
                    TextView txt = new TextView(mActivity);
                    txt.setPadding(2, 2, 2, 2);
                    txt.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                    txt.setText(R.string.no_requests);
                    row.addView(txt);
                    requests_table.addView(row, new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                }
            }
        }
    }

    private Regions getRegionById(Regions[] regions, Integer regionId) {
        if (regionId != null) {
            if (regions != null) {
                for (Regions region : regions) {
                    if (region != null && region.getId().equals(regionId)) return region;
                }
            }
        }
        return null;
    }

    private NewCRequestDetails toRequestDetails(NewCRequest newRequest) {
        NewCRequestDetails nrd = new NewCRequestDetails();
        nrd.setId(newRequest.getId());
        nrd.setRequestsId(newRequest.getRequestsId());
        nrd.setDatecreated(newRequest.getDatecreated());
        nrd.setDispId(newRequest.getDispId());
        nrd.setDispTime(newRequest.getDispTime());
        nrd.setSource(newRequest.getSource());
        nrd.setRegionId(newRequest.getRegionId());
        nrd.setNorth(newRequest.getNorth());
        nrd.setEast(newRequest.getEast());
        nrd.setAddressId(newRequest.getAddressId());
        nrd.setFullAddress(newRequest.getFullAddress());
        nrd.setCarId(newRequest.getCarId());
        nrd.setOperatorId(newRequest.getOperatorId());
        nrd.setAcceptType(newRequest.getAcceptType());
        nrd.setPerformanceTime(newRequest.getPerformanceTime());
        nrd.setWaitingTime(newRequest.getWaitingTime());
        nrd.setDuration(newRequest.getDuration());
        nrd.setStatus(newRequest.getStatus());
        nrd.setRequestGroups(newRequest.getRequestGroups());
        nrd.setPriceGroup(newRequest.getPriceGroup());
        nrd.setProposalTime(newRequest.getProposalTime());
        nrd.setOwner(newRequest.getOwner());
        nrd.setCarNumber(newRequest.getCarNumber());
        nrd.setExecTime(newRequest.getExecTime());
        nrd.setNotes(newRequest.getNotes());
        return nrd;
    }

    @Click
    void backButton() {
        if (history) {
            history = false;
            getRequests();
        } else {
            TaxiApplication.requestsPaused();
            //finish();
            if (mListener != null) mListener.startHome();
        }
    }

    /*@Click
    void newRequests() {
        TaxiApplication.requestsHistory(false);
        TaxiApplication.requestsPaused();
        NewRequestActivity_.intent(this).start();
    }*/

    @Click
    void requestsHistory() {
        history = true;
        requests_table.setVisibility(View.INVISIBLE);
        pbProgress.setVisibility(View.VISIBLE);
        getRequestHistory();
    }
}