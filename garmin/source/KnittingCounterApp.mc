import Toybox.Application;
import Toybox.Communications;
import Toybox.Lang;
import Toybox.WatchUi;

// Remote control for the step counters in the Android app: the phone owns the state, this app shows it and sends taps.
class KnittingCounterApp extends Application.AppBase {
    function initialize() {
        AppBase.initialize();
    }

    function getInitialView() as [WatchUi.Views] or [WatchUi.Views, WatchUi.InputDelegates] {
        var view = new CounterView();
        Communications.registerForPhoneAppMessages(view.method(:onPhoneMessage));
        // Ask for the current state; the phone also pushes every change by itself.
        view.send("sync");
        return [view, new CounterDelegate(view)];
    }

    function onStop(state as Dictionary?) as Void {
        Communications.registerForPhoneAppMessages(null);
    }
}
