import com.github.liuche51.easyTask.test.Test1;
import org.junit.Test;

public class UnitTest {
    @Test
    public void test1() {
        try {
            Test1.getSftpListFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
