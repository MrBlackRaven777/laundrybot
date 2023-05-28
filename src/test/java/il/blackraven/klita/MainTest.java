package il.blackraven.klita;

import il.blackraven.klita.orm.Locales;
import il.blackraven.klita.orm.MachineTypes;
import org.junit.jupiter.api.Test;

public class MainTest {

    @Test
    public void greeterSaysHello() {
        Localisation.init();
//        for(long leftTime = -1; leftTime < 100; leftTime++) {
//            StringBuilder sb = new StringBuilder();
//            if (leftTime < 0) {
//                sb.append(Localisation.getMessage("STATUS_JOB_END", String.valueOf(Locales.ru_RU), MachineTypes.wash));
//            } else if (leftTime > 5 && leftTime < 21) {
//                sb.append(Localisation.getMessage("STATUS_TIME_LEFT_FIVE_TO_TWENTY", String.valueOf(Locales.ru_RU), leftTime));
//            } else if (leftTime % 10 == 1) {
//                sb.append(Localisation.getMessage("STATUS_TIME_LEFT_X_ONE", String.valueOf(Locales.ru_RU), leftTime));
//            } else if ((leftTime % 10 > 1 && leftTime % 10 < 5)) {
//                sb.append(Localisation.getMessage("STATUS_TIME_LEFT_TWO_TO_FOUR", String.valueOf(Locales.ru_RU), leftTime));
//            } else {
//                sb.append(Localisation.getMessage("STATUS_TIME_LEFT_FIVE_TO_TWENTY", String.valueOf(Locales.ru_RU), leftTime));
//            }
//            System.out.println(sb);
//        }
    }
}
