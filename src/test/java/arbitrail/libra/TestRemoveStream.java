package arbitrail.libra;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TestRemoveStream {

	public static void main(String[] args) {
		Set<String> setStr = new HashSet<>();
		setStr.add("a");
		setStr.add("b");
		setStr.add("c");
		
		Map<String, Set<String>> map = new HashMap<>();
		map.put("1", setStr.stream().map(a -> a + "b").collect(Collectors.toSet()));
		map.put("2", setStr.stream().map(a -> a + "b").collect(Collectors.toSet()));
		map.put("3", setStr.stream().map(a -> a + "b").collect(Collectors.toSet()));
		
		System.out.println(map);
		
		map.get("1").remove("ab");
		System.out.println("1: " + map.get("1"));
		System.out.println("2: " + map.get("2"));
		System.out.println("3: " + map.get("3"));
	}

}
