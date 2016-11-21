import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by lrkin on 2016/11/20.
 */
public class LeaderElectionSupport implements Watcher {
    private static final Logger logger = LoggerFactory
            .getLogger(LeaderElectionSupport.class);

    private ZooKeeper zooKeeper;
    private State state;
    private Set<LeaderElectionAware> listeners;
    private String rootNodeName;
    private LeaderOffer leaderOffer;
    private String hostName;

    public LeaderElectionSupport() {
        state = State.STOP;
        Collections.synchronizedSet(new HashSet<LeaderElectionAware>());
    }

    public synchronized void start() {
        state = State.START;
        dispatchEvent(EventType.START);
        logger.info("Starting leader election support");
        if (zooKeeper == null) {
            throw new IllegalStateException(
                    "No instance of zookeeper provided. Hint: use setZooKeeper()"
            );
        }
        if (hostName == null) {
            throw new IllegalStateException(
                    "No hostname provided. Hint: use setHostName()"
            );
        }

        try {
            makeOffer();
            determineElectionStatus();
        } catch (KeeperException e) {
            becomeFailed(e);
            return;
        } catch (InterruptedException e) {
            becomeFailed(e);
            return;
        }

    }

    private void becomeFailed(Exception e) {
        logger.error("Failed in state {} - Exception:{}", state, e);
        state = State.FAILED;
        dispatchEvent(EventType.FAILED);
    }

    /**
     * 决定选举状态
     */
    private void determineElectionStatus() throws KeeperException, InterruptedException {
        state = State.DETERMINE;
        dispatchEvent(EventType.DETERMINE_START);

        String[] components = leaderOffer.getNodePath().split("/");
        leaderOffer.setId(Integer.valueOf(components[components.length - 1].substring("n_".length())));
        List<LeaderOffer> leaderOffers = toLeaderOffers(zooKeeper.getChildren(rootNodeName, false));
        for (int i = 0; i < leaderOffers.size(); i++) {
            LeaderOffer leaderOffer = leaderOffers.get(i);
            if (leaderOffer.getId().equals(this.leaderOffer.getId())) {
                logger.debug("There are {} leader offers. I am {} in line.", leaderOffers.size(), i);
                dispatchEvent(EventType.OFFER_COMPLETE);
                if (i == 0) {
                    becomeLeader();
                } else {
                    becomeReady(leaderOffers.get(i - 1));
                }
                break;
            }
        }
    }

    private List<LeaderOffer> toLeaderOffers(List<String> children) {
    }

    private void makeOffer() throws KeeperException, InterruptedException {
        state = State.OFFER;
        dispatchEvent(EventType.OFFER_START);
        leaderOffer = new LeaderOffer();
        leaderOffer.setHostName(hostName);
        leaderOffer.setNodePath(zooKeeper.create(rootNodeName + "/" + "n_", hostName.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL));
        logger.debug("Created leader offer {}", leaderOffer);
        dispatchEvent(EventType.OFFER_COMPLETE);

    }

    private void dispatchEvent(EventType eventType) {
        logger.debug("Dispatching event:{}", eventType);
        synchronized (listeners) {
            if (listeners.size() > 0) {
                for (LeaderElectionAware observer : listeners) {
                    observer.onElectionEvent(eventType);
                }
            }
        }

    }

    public void process(WatchedEvent watchedEvent) {

    }

    public static enum State {
        START, STOP, DETERMINE, ELECTED, READY, FAILED, OFFER
    }

    public static enum EventType {
        START, OFFER_START, OFFER_COMPLETE, DETERMINE_START, DETERMINE_COMPLETE, ELECTED_START, ELECTED_COMPLETE, READY_START, READY_COMPLETE, FAILED, STOP_START, STOP_COMPLETE,
    }
}
