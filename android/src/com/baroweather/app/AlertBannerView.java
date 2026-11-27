package com.baroweather.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Pinned top banner. Red with a title, reason, WHY? and Ack for warnings; a
 * compact cyan/green strip when pressure is steady or the banner is disabled.
 */
public final class AlertBannerView extends LinearLayout {
    private final ImageView icon;
    private final TextView title;
    private final TextView message;
    private final Button whyButton;
    private final Button ackButton;
    private final View.OnClickListener externalWhy;
    private final View.OnClickListener externalAck;
    private final ThemeColors colors;

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public AlertBannerView(Context context) {
        this(context, null);
    }

    public AlertBannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        colors = new ThemeColors(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClipToPadding(false);

        icon = new ImageView(context);
        icon.setImageResource(R.drawable.ic_warning);
        LayoutParams iconParams = new LayoutParams(dp(28), dp(28));
        iconParams.rightMargin = dp(8);
        addView(icon, iconParams);

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(VERTICAL);
        LayoutParams textParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        addView(texts, textParams);

        title = new TextView(context);
        title.setTextSize(14f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setTextColor(0xFFFFFFFF);
        texts.addView(title, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        message = new TextView(context);
        message.setTextSize(12f);
        message.setTextColor(0xFFFFFFFF);
        texts.addView(message, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        whyButton = new Button(context);
        whyButton.setText(R.string.why);
        whyButton.setTextColor(0xFFFFFFFF);
        whyButton.setTextSize(11f);
        whyButton.setMinimumHeight(dp(48));
        whyButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        ackButton = new Button(context);
        ackButton.setText(R.string.acknowledge);
        ackButton.setTextColor(0xFFFFFFFF);
        ackButton.setTextSize(11f);
        ackButton.setMinimumHeight(dp(48));
        ackButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        LayoutParams buttonParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        addView(whyButton, buttonParams);
        addView(ackButton, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        externalWhy = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Replaced in setAlert.
            }
        };
        externalAck = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Replaced in setAlert.
            }
        };
        whyButton.setOnClickListener(externalWhy);
        ackButton.setOnClickListener(externalAck);
    }

    public void setAlert(PressureAlert alert, boolean bannerEnabled, String valueText,
            View.OnClickListener onWhy, View.OnClickListener onAck) {
        boolean showWarning = alert != null && alert.warning && bannerEnabled;
        int foreground = showWarning ? 0xFFFFFFFF : colors.background;
        title.setTextColor(foreground);
        message.setTextColor(foreground);
        whyButton.setTextColor(foreground);
        ackButton.setTextColor(foreground);
        if (showWarning) {
            setBackgroundResource(R.drawable.bg_banner);
            setMinimumHeight(dp(72));
            icon.setVisibility(VISIBLE);
            title.setText(alert.title);
            message.setText(alert.message + "  " + valueText);
            message.setVisibility(VISIBLE);
            whyButton.setVisibility(VISIBLE);
            ackButton.setVisibility(VISIBLE);
            setContentDescription(alert.title + ". " + alert.message + ". " + valueText);
        } else {
            setBackgroundResource(R.drawable.bg_banner_ok);
            setMinimumHeight(dp(28));
            icon.setVisibility(GONE);
            String text = alert == null ? getResources().getString(R.string.stable_pressure) : alert.title;
            title.setText(text + "  ·  " + valueText);
            message.setVisibility(GONE);
            whyButton.setVisibility(GONE);
            ackButton.setVisibility(GONE);
            setContentDescription(text + ". " + valueText);
        }
        if (onWhy != null) {
            whyButton.setOnClickListener(onWhy);
        }
        if (onAck != null) {
            ackButton.setOnClickListener(onAck);
        }
    }
}
