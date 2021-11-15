package cz.inovatika.sdnnt.model;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Represents different type of periods described in issue 66
 */
public enum Period {

    period_0{
        @Override
        public Date defineDeadline(Date inputDate) {
            // jestli je input date po 1.2 pak 1.8; jinak 1.2; input date je z navrhu
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(inputDate);

            List<Integer> februaryWallMonth = Arrays.asList(9,10,11,12,1);
            List<Integer> augustWallMonth = Arrays.asList(2,3,4,5,6,7,8);

            int month = calendar.get(Calendar.MONTH)+1;

            Calendar retValueCalendar = Calendar.getInstance();
            if (februaryWallMonth.contains(month)) {
                if (month != 1) {
                    retValueCalendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR)+1);
                } else {
                    retValueCalendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
                }
                retValueCalendar.set(Calendar.MONTH, Calendar.FEBRUARY);
            } else if (augustWallMonth.contains(month)) {
                retValueCalendar.set(Calendar.MONTH, Calendar.AUGUST);
                retValueCalendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
            }

            retValueCalendar.set(Calendar.DAY_OF_MONTH, 1);
            retValueCalendar.set(Calendar.DAY_OF_MONTH, 1);

            dayAlign(retValueCalendar);

            return retValueCalendar.getTime();
        }

        @Override
        public DeadlineType getDeadlineType() {
            return DeadlineType.kurator;
        }
    },
    period_1 {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            dayAlign(instance);

            instance.add(Calendar.MONTH, 6);
            return instance.getTime();
        }

        @Override
        public DeadlineType getDeadlineType() {
            return DeadlineType.scheduler;
        }

    },
    period_2 {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            instance.add(Calendar.MONTH, 1);
            dayAlign(instance);

            return instance.getTime();
        }

        @Override
        public DeadlineType getDeadlineType() {
            return DeadlineType.kurator;
        }
    },
    period_3 {
        @Override
        public Date defineDeadline(Date inputDate) {
            //vyřazení ze vzdáleného přístupu (do 5 pracovních dnů – lze požádat nakladatele o prodloužení do 10 pracovních dnů) - Memorandum odst. 11
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            return endOfdayAlign(addWorkingDays(instance,5)).getTime();
        }

        @Override
        public DeadlineType getDeadlineType() {
            return DeadlineType.kurator;
        }
    },
    period_3_1 {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            return endOfdayAlign(addWorkingDays(instance,10)).getTime();
        }

        @Override
        public DeadlineType getDeadlineType() {
            return DeadlineType.kurator;
        }
    },

    period_4 {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            instance.add(Calendar.MONTH, 18);
            return dayAlign(instance).getTime();
        }

        @Override
        public DeadlineType getDeadlineType() {
            return DeadlineType.scheduler;
        }
    };

    private static Calendar dayAlign(Calendar instance) {
        instance.set(Calendar.HOUR, 0);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        instance.set(Calendar.HOUR_OF_DAY, 0);
        return instance;
    }

    private static Calendar endOfdayAlign(Calendar instance) {
        instance.set(Calendar.MINUTE, 59);
        instance.set(Calendar.SECOND, 0);
        instance.set(Calendar.HOUR_OF_DAY, 23);
        return instance;
    }

    private static Calendar addWorkingDays(Calendar instance, int wd) {
        int workingDays = 0;
        while(workingDays < wd) {
            instance.add(Calendar.DAY_OF_MONTH,1);
            int typeOfDay = instance.get(Calendar.DAY_OF_WEEK);
            switch (typeOfDay) {
                case Calendar.MONDAY:
                case Calendar.TUESDAY:
                case Calendar.WEDNESDAY:
                case Calendar.THURSDAY:
                case Calendar.FRIDAY:
                    workingDays +=1;
                break;
            }
        }
        return instance;
    }

    public abstract Date defineDeadline(Date inputDate);

    public abstract DeadlineType getDeadlineType();
}
