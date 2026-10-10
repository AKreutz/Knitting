import Toybox.Attention;
import Toybox.Communications;
import Toybox.Graphics;
import Toybox.Lang;
import Toybox.WatchUi;

// Tells the app whether a message to the phone arrived.
class SendListener extends Communications.ConnectionListener {
    private var mView as CounterView;

    function initialize(view as CounterView) {
        ConnectionListener.initialize();
        mView = view;
    }

    function onComplete() as Void {
        mView.onSendResult(true);
    }

    function onError() as Void {
        mView.onSendResult(false);
    }
}

// Shows the phone's current step: {"p": project, "s": step, "t": type, "prog": n, "max": n, "row": n, "rows": n, "cm": 1},
// or {"idle": 1} when no project is in progress.
class CounterView extends WatchUi.View {
    private var mState as Dictionary? = null;
    private var mIdle as Boolean = false;
    private var mPhoneReachable as Boolean = true;

    function initialize() {
        View.initialize();
    }

    function send(command as String) as Void {
        Communications.transmit({ "cmd" => command }, null, new SendListener(self));
    }

    function onSendResult(delivered as Boolean) as Void {
        mPhoneReachable = delivered;
        WatchUi.requestUpdate();
    }

    function onPhoneMessage(message as Communications.PhoneAppMessage) as Void {
        var data = message.data;
        if (!(data instanceof Dictionary)) {
            return;
        }
        mPhoneReachable = true;
        mIdle = data["idle"] != null;
        if (mIdle) {
            mState = null;
        } else {
            confirmChange(mState, data);
            mState = data;
        }
        WatchUi.requestUpdate();
    }

    // A short buzz for every count, a longer one when a step reaches its target.
    private function confirmChange(old as Dictionary?, now as Dictionary) as Void {
        if (old == null || !(Attention has :vibrate) || !isSameStep(old, now)) {
            return;
        }
        if (old["prog"] == now["prog"] && old["row"] == now["row"]) {
            return;
        }
        var done = now["prog"] != null && now["prog"] == now["max"];
        Attention.vibrate([new Attention.VibeProfile(done ? 100 : 50, done ? 400 : 80)]);
    }

    private function isSameStep(a as Dictionary, b as Dictionary) as Boolean {
        return a["p"] == b["p"] && a["s"] == b["s"];
    }

    function onUpdate(dc as Graphics.Dc) as Void {
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_BLACK);
        dc.clear();
        var w = dc.getWidth();
        var h = dc.getHeight();
        var center = Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER;

        drawButtonLabels(dc, w, h);

        if (mState == null) {
            var message = !mPhoneReachable ? Rez.Strings.NoPhone : (mIdle ? Rez.Strings.Idle : Rez.Strings.Waiting);
            dc.drawText(w / 2, h / 2, Graphics.FONT_SMALL, WatchUi.loadResource(message), center);
            return;
        }

        var state = mState as Dictionary;
        // Only pattern steps come with a row count, so its presence says which kind of step this is.
        var isPattern = state["row"] != null;
        var inCm = state["cm"] != null;

        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.14, Graphics.FONT_XTINY, state["p"], center);
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.25, Graphics.FONT_SMALL, state["s"], center);

        // A pattern step counts rows within the repeat; every other step counts toward its own target.
        var count = isPattern ? state["row"] : state["prog"];
        var target = isPattern ? state["rows"] : state["max"];
        dc.setColor(Graphics.COLOR_ORANGE, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w / 2, h * 0.48, Graphics.FONT_NUMBER_HOT, format(count, inCm && !isPattern), center);
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        var targetText = "/ " + format(target, inCm && !isPattern) + (inCm && !isPattern ? " cm" : "");
        if (isPattern) {
            targetText += " " + WatchUi.loadResource(Rez.Strings.Rows);
        }
        dc.drawText(w / 2, h * (isPattern ? 0.66 : 0.68), Graphics.FONT_SMALL, targetText, center);

        // The repeats (or the length, when that is what the step tracks) are shown beside the rows but only change
        // on the phone: START and DOWN count rows.
        if (isPattern) {
            var progress = inCm
                ? format(state["prog"], true) + " / " + format(state["max"], true) + " cm"
                : WatchUi.loadResource(Rez.Strings.Repeats) + " " + state["prog"] + " / " + state["max"];
            dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
            dc.drawText(w / 2, h * 0.82, Graphics.FONT_SMALL, progress, center);
        }

        if (!mPhoneReachable) {
            dc.setColor(Graphics.COLOR_RED, Graphics.COLOR_TRANSPARENT);
            dc.drawText(w / 2, h * 0.92, Graphics.FONT_XTINY, WatchUi.loadResource(Rez.Strings.NoPhone), center);
        }
    }

    // Labels on the screen next to the buttons: START (top right) and BACK (bottom right) on the right side,
    // UP (middle) and DOWN (bottom) on the left. Only the buttons that do something right now are labeled.
    private function drawButtonLabels(dc as Graphics.Dc, w as Number, h as Number) as Void {
        var left = Graphics.TEXT_JUSTIFY_LEFT | Graphics.TEXT_JUSTIFY_VCENTER;
        var right = Graphics.TEXT_JUSTIFY_RIGHT | Graphics.TEXT_JUSTIFY_VCENTER;
        var font = Graphics.FONT_TINY;
        dc.setColor(Graphics.COLOR_LT_GRAY, Graphics.COLOR_TRANSPARENT);
        dc.drawText(w * 0.9, h * 0.72, font, WatchUi.loadResource(Rez.Strings.Back), right);
        dc.drawText(w * 0.06, h * 0.5, font, WatchUi.loadResource(Rez.Strings.Resync), left);
        if (mState != null) {
            var state = mState as Dictionary;
            var millimeters = state["cm"] != null && state["row"] == null;
            dc.setColor(Graphics.COLOR_ORANGE, Graphics.COLOR_TRANSPARENT);
            var inc = state["inc"] != null ? state["inc"] : 1;
            var dec = state["dec"] != null ? state["dec"] : 1;
            dc.drawText(w * 0.9, h * 0.28, font, "+" + format(inc, millimeters), right);
            dc.drawText(w * 0.14, h * 0.72, font, "-" + format(dec, millimeters), left);
        }
    }

    // Millimeters arrive as whole numbers and show as centimeters with one decimal.
    private function format(value as Number?, millimeters as Boolean) as String {
        if (value == null) {
            return "";
        }
        return millimeters ? (value / 10.0).format("%.1f") : value.toString();
    }
}
