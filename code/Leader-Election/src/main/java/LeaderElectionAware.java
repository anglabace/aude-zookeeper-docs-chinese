/**
 * Created by lrkin on 2016/11/20.
 */
public interface LeaderElectionAware {
    public void onElectionEvent(LeaderElectionSupport.EventType eventType);
}
