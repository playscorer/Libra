package arbitrail.libra.service;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
  {"/spring-test.xml"})
public class CipherServiceTest {
	
	@Autowired
	private CipherService svc;
	
	@Test
	public void testDecrypt() {
		String result = svc.decrypt("tK+pyPaSuI0tlEHMNfUiDEGCfr41f42u/p5QE0vdrMYWqPxuW1X7ff7QHywjbEOX", "2ax2V891ankOBf7kC89ox0f0aYfTjYTqn6sc9TesMLX");
		System.out.println(result);
		assertTrue(true);
	}
	
}
