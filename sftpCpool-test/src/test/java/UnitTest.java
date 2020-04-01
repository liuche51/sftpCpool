import com.github.liuche51.easyTask.test.Test1;
import org.junit.Test;

import java.util.List;

public class UnitTest {
    @Test
    public void test1() {
        try {
            List<String> list= Test1.getSftpListFiles();
            for(String s:list){
                System.out.println(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
