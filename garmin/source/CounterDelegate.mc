import Toybox.Lang;
import Toybox.WatchUi;

// START counts up, DOWN counts back and UP asks the phone for the state again; BACK leaves the app.
class CounterDelegate extends WatchUi.BehaviorDelegate {
    private var mView as CounterView;

    function initialize(view as CounterView) {
        BehaviorDelegate.initialize();
        mView = view;
    }

    function onSelect() as Boolean {
        mView.send("inc");
        return true;
    }

    function onNextPage() as Boolean {
        mView.send("dec");
        return true;
    }

    function onPreviousPage() as Boolean {
        mView.send("sync");
        return true;
    }
}
