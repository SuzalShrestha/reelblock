package shots;
import org.robolectric.annotation.Config;
// Same scenes on Android 11: no dynamic colour, so the fixed brand tokens (teal) show.
@Config(sdk = 30)
public class ShotsR extends Shots {}
