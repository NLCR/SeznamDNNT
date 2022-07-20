package cz.inovatika.sdnnt.model;

import cz.inovatika.sdnnt.Options;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Represents different type of periods described in issue 66
 */
public enum Period {

    debug_nzn_0_5wd {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            return addMinutes(instance,periodValue(this.name(), 5)).getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.kurator;
        }
    },

    period_nzn_0_5wd {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            return endOfdayAlign(addWorkingDays(instance,periodValue(this.name(), 5))).getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.kurator;
        }
    },

    debug_nzn_1_12_18 {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            return addMinutes(instance,periodValue(this.name(), 5)).getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.scheduler;
        }
    },

    period_nzn_1_12_18 {
        @Override
        public Date defineDeadline(Date inputDate) {
            // nema smysl to delat konfiguratovatlne ?
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(inputDate);

            List<Integer> februaryWallMonth = Arrays.asList(8,9,10,11,12,1);
            List<Integer> augustWallMonth = Arrays.asList(2,3,4,5,6,7);

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
        public TransitionType getTransitionType() {
            return TransitionType.scheduler;
        }
    },

    debug_nzn_2_6m {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            instance.add(Calendar.MINUTE, periodValue(this.name(),6));
            return instance.getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.scheduler;
        }
    },
    period_nzn_2_6m {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            dayAlign(instance);

            instance.add(Calendar.MONTH, periodValue(this.name(),6));
            return instance.getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.scheduler;
        }

    },
    debug_vn_0_28d {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            instance.add(Calendar.MINUTE, periodValue(this.name(),8));
            return instance.getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.kurator;
        }
    },
    period_vn_0_28d {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            // 28 dnu, ne cely mesic
            instance.add(Calendar.DAY_OF_MONTH, periodValue(this.name(),28));
            //instance.add(Calendar.MONTH, 1);
            dayAlign(instance);

            return instance.getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.kurator;
        }
    },

    debug_vnl_0_5wd {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            instance.add(Calendar.MINUTE, periodValue(this.name(),5));
            return instance.getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.kurator;
        }
    },
    period_vln_0_5wd {
        @Override
        public Date defineDeadline(Date inputDate) {
            //vyřazení ze vzdáleného přístupu (do 5 pracovních dnů – lze požádat nakladatele o prodloužení do 10 pracovních dnů) - Memorandum odst. 11
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            return endOfdayAlign(addWorkingDays(instance,periodValue(this.name(),5))).getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.kurator;
        }
    },
    debug_vnl_1_10wd {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            instance.add(Calendar.MINUTE, periodValue(this.name(),5));
            return instance.getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.kurator;
        }
    },
    period_vnl_1_10d {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            return endOfdayAlign(addWorkingDays(instance,periodValue(this.name(),10))).getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.kurator;
        }
    },
    debug_vnl_2_18m {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            instance.add(Calendar.MINUTE, periodValue(this.name(),8));
            return instance.getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.scheduler;
        }
    },
    period_vln_2_18m {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            instance.add(Calendar.MONTH, periodValue(this.name(),18));
            return dayAlign(instance).getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.scheduler;
        }
    },

    debug_vnl_3_5wd  {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            instance.add(Calendar.MINUTE, periodValue(this.name(),5));
            return instance.getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.kurator;
        }
    },
    period_vln_3_5wd {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            return endOfdayAlign(addWorkingDays(instance,periodValue(this.name(), 5))).getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.scheduler;
        }
    },


    debug_px_0_5wd {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            instance.add(Calendar.MINUTE, periodValue(this.name(),5));
            return instance.getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.kurator;
        }
    },
    period_px_0_5wd {
        @Override
        public Date defineDeadline(Date inputDate) {
            //vyřazení ze vzdáleného přístupu (do 5 pracovních dnů – lze požádat nakladatele o prodloužení do 10 pracovních dnů) - Memorandum odst. 11
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            return endOfdayAlign(addWorkingDays(instance,periodValue(this.name(),5))).getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.kurator;
        }
    },
    
    
    debug_dx_0_5wd {
        @Override
        public Date defineDeadline(Date inputDate) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            instance.add(Calendar.MINUTE, periodValue(this.name(),5));
            return instance.getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.kurator;
        }
    },
    period_dx_0_5wd {
        @Override
        public Date defineDeadline(Date inputDate) {
            //vyřazení ze vzdáleného přístupu (do 5 pracovních dnů – lze požádat nakladatele o prodloužení do 10 pracovních dnů) - Memorandum odst. 11
            Calendar instance = Calendar.getInstance();
            instance.setTime(inputDate);
            return endOfdayAlign(addWorkingDays(instance,periodValue(this.name(),5))).getTime();
        }

        @Override
        public TransitionType getTransitionType() {
            return TransitionType.kurator;
        }
    },
    ;


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


    static Calendar addWorkingDays(Calendar instance, int wd) {
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
                    if (!publicDay(instance.get(Calendar.DAY_OF_MONTH), instance.get(Calendar.MONTH))) {
                        workingDays +=1;
                    }
                break;
            }
        }
        return instance;
    }

    static Calendar addMinutes(Calendar instance, int mnt) {
        instance.add(Calendar.MINUTE, mnt);
        return instance;
    }



    static boolean publicDay(int day, int month) {
        Options opts = Options.getInstance();
        if (opts.getJSONObject("workflow").has("holidays")) {
            JSONArray holidays = opts.getJSONObject("workflow").getJSONArray("holidays");
            for (int i = 0; i < holidays.length(); i++) {
                JSONObject holiday = holidays.getJSONObject(i);
                if (holiday.has("day") && holiday.has("month")) {
                    int d = holiday.getInt("day");
                    int m = holiday.getInt("month");
                    return d == day && month == m;
                }

            }
        }
        return false;
    }


    static int periodValue(String periodname, int defaultval) {
        Options opts = Options.getInstance();
        if (opts.getJSONObject("workflow").has("periods")) {
            JSONObject periods = opts.getJSONObject("workflow").getJSONObject("periods");
            if (periods.has(periodname) &&  periods.getJSONObject(periodname).has("value")) {
                return periods.getJSONObject(periodname).getInt("value");
            }

        }
        return defaultval;
    }

    public abstract Date defineDeadline(Date inputDate);

    public abstract TransitionType getTransitionType();
}
