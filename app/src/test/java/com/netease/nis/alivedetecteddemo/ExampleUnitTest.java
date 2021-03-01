package com.netease.nis.alivedetecteddemo;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    public enum ActionType {
        ACTION_STRAIGHT_AHEAD("0", "正视前方"),
        ACTION_TURN_HEAD_TO_RIGHT("1", "向右转头"),
        ACTION_TURN_HEAD_TO_LEFT("2", "向左转头"),
        ACTION_OPEN_MOUTH("3", "张嘴动作"),
        ACTION_BLINK_EYES("4", "眨眼动作");
        String actionId;
        String actionTip;

        private ActionType(String actionType, String tip) {
            this.actionId = actionType;
            this.actionTip = tip;
        }

        String getActionID() {
            return actionId;
        }

        String getActionTip() {
            return actionTip;
        }
    }

    @Test
    public void test() {
        ActionType actionType = ActionType.ACTION_OPEN_MOUTH;
        System.out.print(actionType.getActionID() + actionType.getActionTip());
    }
}