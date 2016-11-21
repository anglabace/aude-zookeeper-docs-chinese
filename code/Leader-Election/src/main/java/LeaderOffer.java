import java.util.Comparator;

/**
 * Created by lrkin on 2016/11/20.
 */
public class LeaderOffer {
    private Integer id;
    private String nodePath;
    private String hostName;

    public LeaderOffer() {
    }

    public LeaderOffer(Integer id, String nodePath, String hostName) {
        this.id = id;
        this.nodePath = nodePath;
        this.hostName = hostName;
    }

    @Override
    public String toString() {
        return "{ id:" + id + " nodePath:" + nodePath + " hostName:" + hostName
                + " }";
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public static class IdComparator implements Comparator<LeaderOffer> {

        public int compare(LeaderOffer o1, LeaderOffer o2) {
            return o1.getId().compareTo(o2.getId());
        }
    }
}
