package androidx.test.internal.runner.lifecycle;
import androidx.test.runner.lifecycle.*; import android.app.Activity; import java.util.*;
public class ActivityLifecycleMonitorImpl implements ActivityLifecycleMonitor {
  final Map<Activity, Stage> m = new WeakHashMap<>();
  public void signalLifecycleChange(Stage s, Activity a) { m.put(a, s); }
  public void addLifecycleCallback(ActivityLifecycleCallback c) {} public void removeLifecycleCallback(ActivityLifecycleCallback c) {}
  public Stage getLifecycleStageOf(Activity a) { return m.get(a); }
  public Collection<Activity> getActivitiesInStage(Stage s) { List<Activity> l = new ArrayList<>(); for (Map.Entry<Activity, Stage> e : m.entrySet()) if (e.getValue() == s) l.add(e.getKey()); return l; }
}
