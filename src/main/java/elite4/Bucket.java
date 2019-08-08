import java.util.List;

public class Bucket {
	
	private List<String> members;
	
	public void addMember(String member) {
		members.add(member);
	}
	
	public void removeMember(String member) {
		members.remove(member);
	}
	
	public List<String> getMembers() {
		return members;
	}
	
}