package com.pawelszczerbiak.smog;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class StationAdapter extends ArrayAdapter<Station> {

    public StationAdapter(@NonNull Context context, int resource, @NonNull List<Station> stations) {
        super(context, resource, stations);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.station_list_item, parent, false);
        }

        Station currentStation = getItem(position);

        Map<String, String> dates = currentStation.getDates();
        Map<String, Double> pollutions = currentStation.getPollutions();
        String type = currentStation.getType();

        // Views to be changed
        TextView locationView = (TextView) listItemView.findViewById(R.id.location);
        TextView dateView = (TextView) listItemView.findViewById(R.id.date);
        // OLD IDEA: Changes layout color depending on the station's type
        // LinearLayout stationInfoLayout = (LinearLayout) listItemView.findViewById(R.id.stationInfo);
        // stationInfoLayout.setBackgroundColor(ContextCompat.getColor(getContext(), getLayoutColor(type)));
        // NEW IDEA: Changes text and text color of the station's type label
        TextView typeView = (TextView) listItemView.findViewById(R.id.type);
        typeView.setText(getTypeText(type));
        typeView.setTextColor(ContextCompat.getColor(getContext(), getTypeColor(type)));

        /**
         * Format data from current station: date
         * First date is for PM2.5 which is more important than PM10 etc.
         * Note: dates are ordered according to IDs @stationsData
         * but we can change ordering by initialization the data map
         */
        Map.Entry<String, String> firstDate = dates.entrySet().iterator().next();
        String formattedDate = formatDate(firstDate.getValue());
        // Insert formatted data into the views
        locationView.setText(currentStation.getLocation());
        dateView.setText(formattedDate);

        /**
         *  Format data from current station: pollutions
         */
        for (String key : pollutions.keySet()) {
            switch (key) {
                case "PM2.5":
                    changePollutionView(listItemView, pollutions.get(key),
                            R.id.val_PM25, R.id.label_PM25,
                            PollutionNorms.TABLE_REF_PM25, PollutionNorms.NORM_PM25);
                    break;
                case "PM10":
                    changePollutionView(listItemView, pollutions.get(key),
                            R.id.val_PM10, R.id.label_PM10,
                            PollutionNorms.TABLE_REF_PM10, PollutionNorms.NORM_PM10);
                    break;
                case "C6H6":
                    changePollutionView(listItemView, pollutions.get(key),
                            R.id.val_C6H6, R.id.label_C6H6,
                            PollutionNorms.TABLE_REF_C6H6, PollutionNorms.NORM_C6H6);
                    break;
                case "SO2":
                    changePollutionView(listItemView, pollutions.get(key),
                            R.id.val_SO2, R.id.label_SO2,
                            PollutionNorms.TABLE_REF_SO2, PollutionNorms.NORM_SO2);
                    break;
                case "NO2":
                    changePollutionView(listItemView, pollutions.get(key),
                            R.id.val_NO2, R.id.label_NO2,
                            PollutionNorms.TABLE_REF_NO2, PollutionNorms.NORM_NO2);
                    break;
            }
        }

        // Return the list item view that is now showing the appropriate data
        return listItemView;
    }

    /**
     * Changes views for specific pollution
     */
    private void changePollutionView(View listItemView, double pollutionValue, int idValue, int idLabel, int[] tableRef, int norm) {
        // Views to be changed
        TextView valueView = (TextView) listItemView.findViewById(idValue);
        TextView labelView = (TextView) listItemView.findViewById(idLabel);
        // Rectangle for specific pollutionValue
        GradientDrawable valueRectangle = (GradientDrawable) valueView.getBackground();
        // Get the appropriate background color based on the current pollutionValue
        int ValueColor = getValueColor(pollutionValue, tableRef);
        valueRectangle.setColor(ValueColor);
        // Set text and label color
        valueView.setTextColor(ContextCompat.getColor(getContext(), getValueTextColor(pollutionValue)));
        labelView.setTextColor(ContextCompat.getColor(getContext(), getLabelTextColor(pollutionValue)));
        // Format data from current station
        String formattedMag = formatValue(pollutionValue, norm);
        // Insert formatted data into the view
        valueView.setText(formattedMag);
    }

    /**
     * Formats value
     */
    private String formatValue(double value, int norm) {
        if (value >= 0)
            return String.valueOf((int) Math.round(100.0 * value / norm)) + "%";
        else
            return String.valueOf(R.string.no_data);
    }

    /**
     * Gives text color for specific value
     */
    private int getValueTextColor(double value) {
        if (value >= 0)
            return R.color.textColorValue;
        else
//         return R.color.textColorValueDefault;
            return R.color.colorValueNone;
    }

    /**
     * Gives text color for specific label
     */
    private int getLabelTextColor(double value) {
        if (value >= 0)
            return R.color.textColorValueDefault;
        else
            return R.color.colorValueNone;
    }

    /**
     * Formats date
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String formatDate(String oldDateString) {

        // Old date format
        DateFormat oldFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date oldDate = null;
        try {
            oldDate = oldFormat.parse(oldDateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // New date format
        DateFormat newFormat = new SimpleDateFormat("HH:mm");
        String newDateString = newFormat.format(oldDate);

        return newDateString;
    }

    /**
     * Gives value color for specific pollution
     *
     * @param value    given value
     * @param tableRef reference table for colors
     */
    private int getValueColor(double value, int[] tableRef) {
        int valueColorResourceId;
        int valueRound = (int) Math.round(value);
        if (valueRound > tableRef[4]) // very bad
            valueColorResourceId = ContextCompat.getColor(getContext(), R.color.colorValueVeryBad);
        else if (valueRound > tableRef[3]) // bad
            valueColorResourceId = ContextCompat.getColor(getContext(), R.color.colorValueBad);
        else if (valueRound > tableRef[2]) // sufficient
            valueColorResourceId = ContextCompat.getColor(getContext(), R.color.colorValueSufficient);
        else if (valueRound > tableRef[1]) // moderate
            valueColorResourceId = ContextCompat.getColor(getContext(), R.color.colorValueModerate);
        else if (valueRound > tableRef[0]) // good
            valueColorResourceId = ContextCompat.getColor(getContext(), R.color.colorValueGood);
        else if (valueRound >= 0) // very good
            valueColorResourceId = ContextCompat.getColor(getContext(), R.color.colorValueVeryGood);
        else // if there is a problem e.g. negative value
            valueColorResourceId = ContextCompat.getColor(getContext(), R.color.colorValueNone);

        return valueColorResourceId;
    }

    /**
     * Gives color for specific location's type
     */
    private int getTypeColor(String type) {
        switch (type) {
            case "Ważne miejscowości":
                return R.color.colorImportantCities;
            case "Podhale":
                return R.color.colorPodhale;
            case "Beskidy Zachodnie":
                return R.color.colorBeskidyZachodnie;
            case "Beskidy Wschodnie":
                return R.color.colorBeskidyWschodnie;
            case "Sudety":
                return R.color.colorSudety;
            case "Jura":
                return R.color.colorJura;
        }
        return R.color.colorValueNone;
    }

    /**
     * Gives text for specific location's type
     */
    private int getTypeText(String type) {
        switch (type) {
            case "Ważne miejscowości":
                return R.string.stringImportantCities;
            case "Podhale":
                return R.string.stringPodhale;
            case "Beskidy Zachodnie":
                return R.string.stringBeskidyZachodnie;
            case "Beskidy Wschodnie":
                return R.string.stringBeskidyWschodnie;
            case "Sudety":
                return R.string.stringSudety;
            case "Jura":
                return R.string.stringJura;
        }
        return R.string.no_data;
    }
}