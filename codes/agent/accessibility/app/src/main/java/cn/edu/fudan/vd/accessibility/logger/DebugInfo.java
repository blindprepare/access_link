package cn.edu.fudan.vd.accessibility.logger;

import android.view.View;
import android.view.ViewGroup;

import java.io.Serializable;
import java.util.List;

interface DebugInfoInterface {
    String generateMessage(Object... args);
}

public enum DebugInfo implements DebugInfoInterface {
    NATIVE_METHOD_INVOCATION {
        @Override
        public String generateMessage(Object... args) {
            return "This method is invoked.";
        }
    },
    THRID_PARTY_METHOD_INVOCATION {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{String.class}, args))
                return "The method [name = unknown] is invoked.";
            String methodName = (String) args[0];
            return String.format("The method [name = %s] is invoked.", methodName);
        }
    },
    THRID_PARTY_METHOD_RETURN {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{String.class}, args))
                return "The method [name = unknown] returns.";
            String methodName = (String) args[0];
            return String.format("The method [name = %s] returns.", methodName);
        }
    },
    CURRENT_WINDOW_NULL {
        @Override
        public String generateMessage(Object... args) {
            return "Current window is null.";
        }
    },
    LIST_ROOT_VIEW_INFO {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{View.class}, args))
                return "The info of list root view: [invalid args].";
            View listRootView = (View) args[0];
            int[] position = new int[2];
            listRootView.getLocationOnScreen(position);
            String listRootType = listRootView.getClass().getName();
            int listRootWidth = listRootView.getWidth();
            int listRootHeight = listRootView.getHeight();
            int listRootChildNum = 0;
            if (listRootView instanceof ViewGroup)
                listRootChildNum = ((ViewGroup) listRootView).getChildCount();
            String logInfo = "The info of list root view: [type = %s, X = %d, Y = %d, width = %d, height = %d, child num = %d].";
            return String.format(logInfo, listRootType, position[0], position[1], listRootWidth, listRootHeight, listRootChildNum);
        }
    },
    TARGET_VIEW_NOT_FOUND {
        @Override
        public String generateMessage(Object... args) {
            return "Fail to find the target view.";
        }
    },
    TARGET_VIEW_FOUND {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{View.class}, args))
                return "Find target view.";
            View view = (View) args[0];
            return String.format("Find target view [type = %s] successfully.", view.getClass().getName());
        }
    },
    TARGET_VIEW_INVALID_TYPE {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{View.class}, args))
                return "The target view type is incorrect.";
            View view = (View) args[0];
            return String.format("The target view [type = %s] is incorrect.", view.getClass().getName());
        }
    },
    LIST_ROOT_VIEW_NOT_FOUND {
        @Override
        public String generateMessage(Object... args) {
            return "Fail to locate the root view of the list.";
        }
    },
    LIST_ROOT_VIEW_INVALID_TYPE {
        @Override
        public String generateMessage(Object... args) {
            return "The root view of the list is not an instance of ViewGroup.";
        }
    },
    LIST_ROOT_VIEW_LEAST_ITEM_NUM {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "The least item num of the root view of the list is unknown.";
            int leastItemNum = (Integer) args[0];
            return String.format("The least item num of the root view of the list is %d.", leastItemNum);
        }
    },
    LIST_ROOT_VIEW_LOCATION {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class, Integer.class}, args))
                return "The location of list root view is unknown.";
            int positionX = (Integer) args[0];
            int positionY = (Integer) args[1];
            return String.format("The location of list root view is (%d, %d).", positionX, positionY);
        }
    },
    LIST_LIKE_VIEW_LOADED {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "The list-like view has been loaded successfully.";
            int childCount = (Integer) args[0];
            return String.format("The list-like view [child num = %d] has been loaded successfully.", childCount);
        }
    },
    LIST_LIKE_VIEW_LOADING_CONTINUATION {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class, Integer.class}, args))
                return "The list-like view is not loaded compeletely.";
            int expectedChildNum = (Integer) args[0];
            int actualChildNum = (Integer) args[1];
            String logString = "The list-like view is not loaded compeletely [expected child num = %d, actual child num = %d].";
            return String.format(logString, expectedChildNum, actualChildNum);
        }
    },
    LIST_LIKE_VIEW_LOADING_TIMEOUT {
        @Override
        public String generateMessage(Object... args) {
            return "Timeout for loading list-like view.";
        }
    },
    FIND_VIEW_WITH_DUPLATE_ID {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "Try to find the target view with path tree since its ID is not unique.";
            int viewID = (Integer) args[0];
            return String.format("Try to find the target view with path tree since its ID [value = %d] is not unique.", viewID);
        }
    },
    FIND_VIEW_INCORRECT_ACTIVITY {
        @Override
        public String generateMessage(Object... args) {
            return "The acitivity of the target view is not the same as the current one.";
        }
    },
    FIND_VIEW_INCORRECT_DIALOG {
        @Override
        public String generateMessage(Object... args) {
            return "The acitivity of the target view is not the same as the current one.";
        }
    },
    FIND_VIEW_WITH_INVALID_SYSTEM_ID {
        @Override
        public String generateMessage(Object... args) {
            return "Try to find the target view with path tree since system-assigned ID does not work.";
        }
    },
    UPDATE_UI {
        @Override
        public String generateMessage(Object... args) {
            return "Wait for updating UI (in UI thread).";
        }
    },
    REPLAY_EVENT_FINISH {
        @Override
        public String generateMessage(Object... args) {
            return "Finish performing replay action successfully.";
        }
    },
    FILL_IN_TEXT_CONTENT {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{String.class}, args))
                return "Fill in the editText with content.";
            String text = (String) args[0];
            return String.format("Fill in the editText with content [value = %s].", text);
        }
    },
    DECREASE_COUNT_DOWN_LATCH {
        @Override
        public String generateMessage(Object... args) {
            return "Decrease the value of CountDownLatch object.";
        }
    },
    FIXED_INDEX_ITEM_NOT_FOUND {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "Fail to locate the item in the list.";
            int index = (Integer) args[0];
            return String.format("Fail to locate the item [index = %d] in the list.", index);
        }
    },
    FIXED_INDEX_ITEM_FOUND {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class, View.class}, args))
                return "Locate the item in the list.";
            int index = (Integer) args[0];
            View view = (View) args[1];
            return String.format("Locate the item [index = %d, type = %s] in the list successfully.", index, view.getClass().getName());
        }
    },
    FIXED_INDEX_ITEM_INDEX {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "The fixed item index is unknown.";
            int index = (Integer) args[0];
            return String.format("The fixed item index is %d.", index);
        }
    },
    FIXED_CONTENT_ITEM_NOT_FOUND {
        @Override
        public String generateMessage(Object... args) {
            return "Fail to locate the item that matches the given contents";
        }
    },
    FIXED_CONTENT_ITEM_MATCHING_TEXT {
        @SuppressWarnings("unchecked")
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{List.class}, args))
                return "The matching text(s) for fixed content item are unknown.";
            List<String> list = (List<String>) args[0];
            return String.format("The matching text(s) for fixed content item : %s.", printLogInfo(list));
        }

        private String printLogInfo(List<String> list) {
            String result = "[";
            for (int i = 0; i < list.size(); i++) {
                result += String.format("text[%d] = %s", i, list.get(i));
                if (i != list.size() - 1)
                    result += ", ";
            }
            result += "]";
            return result;
        }
    },
    KEY_EVENT_CODE_NULL {
        @Override
        public String generateMessage(Object... args) {
            return "The key code of KeyEnterEvent is null.";
        }
    },
    OUTPUT_EXPECTED_TARGET_VIEW_NUM {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "The expected number of target view(s) to read is unknown.";
            int num = (Integer) args[0];
            return String.format("The expected number of target view(s) to read is %d.", num);
        }
    },
    OUTPUT_ACTUAL_FOUND_VIEW_NUM {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "The actual number of target view(s) to read is unknown.";
            int num = (Integer) args[0];
            return String.format("The actual number of target view(s) to read is %d.", num);
        }
    },
    OUTPUT_TARGET_INDEX_VIEW_NOT_FOUND {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "Fail to find the target view to read.";
            int index = (Integer) args[0];
            return String.format("Fail to find the target view [index = %d] to read.", index);
        }
    },
    OUTPUT_TARGET_INDEX_VIEW_FOUND {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class, View.class}, args))
                return "Find the target view to read successfully.";
            int index = (Integer) args[0];
            View view = (View) args[1];
            return String.format("Find the target view [index = %d, type = %s] to read successfully.", index, view.getClass().getName());
        }
    },
    ITEM_LOCATION_DATA {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Serializable.class}, args))
                return "The list location data is unknown.";
            Serializable itemLocationData = (Serializable) args[0];
            if (itemLocationData == null)
                return "The list location data is null.";
            else
                return String.format("The list location data [object = %s] is not null.", itemLocationData.toString());
        }
    },
    LIST_ITEM_NUM {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "The item number of the list is unknown.";
            int num = (Integer) args[0];
            return String.format("The list contains %d items(s).", num);
        }
    },
    SCROLL_NO_NEW_ITEM {
        @Override
        public String generateMessage(Object... args) {
            return "No new items are displayed after scrolling.";
        }
    },
    SCROLL_ITEM_NUMBER {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "The item number of the list after scrolling is unknown.";
            int num = (Integer) args[0];
            return String.format("%d item(s) are displayed and remained to be read out after scrolling.", num);
        }
    },
    SCROLL_EVENT_START_POSITION {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Float.class, Float.class}, args))
                return "The start position of scroll event is unknown.";
            float positionX = (float) args[0];
            float positionY = (float) args[1];
            return String.format("The start position of scroll event is located at (%.1f, %.1f).", positionX, positionY);
        }
    },
    FILTERED_ITEM_LIST_NULL {
        @Override
        public String generateMessage(Object... args) {
            return "The filtered item list is null.";
        }
    },
    FILTERED_ITEM_LIST_NUM {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "The number of filtered item list is unknown.";
            int num = (Integer) args[0];
            return String.format("Find %d item(s) that matches filter conditions.", num);
        }
    },
    ITEM_MATCHING_FILTER_CONDITION {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "The item is contained in target item list.";
            int index = (Integer) args[0];
            return String.format("The item [index = %d] is contained in target item list.", index);
        }
    },
    ITEM_NOT_MATCHING_FILTER_CONDITION {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "The item is not contained in target item list.";
            int index = (Integer) args[0];
            return String.format("The item [index = %d] is not contained in target item list.", index);
        }
    },
    ITEM_CENTER_NOT_DISPLAYED {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "The view center (for clicking) is not displayed in the screen.";
            int index = (Integer) args[0];
            return String.format("The view [index = %d] center (for clicking) is not displayed in the screen.", index);
        }
    },
    FILTER_CONDITION_EXPECTED_NUM {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class}, args))
                return "The expected filter condition num is unknown.";
            int num = (Integer) args[0];
            return String.format("The expected filter condition num is %d.", num);
        }
    },
    FILTER_CONDITION_ASK_USER {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class, String.class}, args))
                return "Ask filter condition from user.";
            int num = (Integer) args[0];
            String prompt = (String) args[1];
            return String.format("Ask filter condition [index = %d, prompt = %s] from user.", num, prompt);
        }
    },
    FILTER_CONDITION_USER_RESPONSE {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{String.class}, args))
                return "The filter condition provided by user is unknown.";
            String condition = (String) args[0];
            return String.format("The user provides '%s' as condition.", condition);
        }
    },
    FILTER_CONDITION_NO_RESPONSE {
        @Override
        public String generateMessage(Object... args) {
            return "The user provides no condition.";
        }
    },
    FILTER_CONDITION_LIST {
        @SuppressWarnings("unchecked")
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{List.class}, args))
                return "The conditionThe condition list is unknown.";
            List<String> list = (List<String>) args[0];
            return String.format("The condition list : %s.", printLogInfo(list));
        }

        private String printLogInfo(List<String> list) {
            String result = "[";
            for (int i = 0; i < list.size(); i++) {
                result += String.format("condition[%d] = %s", i, list.get(i));
                if (i != list.size() - 1)
                    result += ", ";
            }
            result += "]";
            return result;
        }
    },
    FILTER_CONDITION_PROMPT_NO_RESPONSE {
        @Override
        public String generateMessage(Object... args) {
            return "Fail to get prompt from user, use default prompt instead.";
        }
    },
    NO_ITEM_SELECTED_BY_USER {
        @Override
        public String generateMessage(Object... args) {
            return "No item is selected by the user.";
        }
    },
    START_THREAD {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Class.class}, args))
                return "Start thread/action [class = unknown].";
            Class<?> clazz = (Class<?>) args[0];
            return String.format("Start thread/action [class = %s].", clazz.getSimpleName());
        }
    },
    WAIT_FOR_RUNNING_THREAD {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Class.class}, args))
                return "Wait for running thread/action [class = unknown].";
            Class<?> clazz = (Class<?>) args[0];
            return String.format("Wait for running thread/action [class = %s].", clazz.getSimpleName());
        }
    },
    PHONE_SCREEN_SIZE {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class, Integer.class}, args))
                return "The screen size is unknown.";
            int width = (Integer) args[0];
            int height = (Integer) args[1];
            return String.format("The screen size is [width = %d, height = %d].", width, height);
        }
    },
    VIEW_CENTER_LOCATION {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{Integer.class, Integer.class}, args))
                return "The view center is unknown.";
            int positionX = (Integer) args[0];
            int positionY = (Integer) args[1];
            return String.format("The view center is located at (%d, %d).", positionX, positionY);
        }
    },
    PERFORM_VIEW_CLICK_DIRECTLY {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{View.class}, args))
                return "perform click event on the view.";
            View view = (View) args[0];
            return String.format("Perform click event [type = %s] on the view.", view.getClass().getName());
        }
    },
    SIMULATE_VIEW_CLICK_VIA_MOTION {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{View.class}, args))
                return "Simulate click event on the view.";
            View view = (View) args[0];
            return String.format("Simulate click event [type = %s] on the view.", view.getClass().getName());
        }
    },
    SIMULATE_MOTION_EVENT {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{String.class, Float.class, Float.class}, args))
                return "Simulate event [X = unknown, Y = known].";
            String eventName = (String) args[0];
            float positionX = (float) args[1];
            float positionY = (float) args[2];
            return String.format("Simulate %s event [X = %.1f, Y = %.1f].", eventName, positionX, positionY);
        }
    },
    VARIABLE_TEXT_USER_INPUT {
        @Override
        public String generateMessage(Object... args) {
            if (!checkArgumentType(new Class<?>[]{String.class}, args))
                return "The user provide input for variable text.";
            String text = (String) args[0];
            return String.format("The user provide '%s' as input for variable text.", text);
        }
    },
    VARIABLE_TEXT_NO_USER_INPUT {
        @Override
        public String generateMessage(Object... args) {
            return "The user does not provide any content for variable text.";
        }
    },
    INPUT_METHOD_MANAGER_NULL {
        @Override
        public String generateMessage(Object... args) {
            return "The InputMethodManager object is null.";
        }
    },
    DISPLAY_SOFT_KEYBOARD {
        @Override
        public String generateMessage(Object... args) {
            return "Display soft keyboard.";
        }
    },
    HIDE_SOFT_KEYBOARD {
        @Override
        public String generateMessage(Object... args) {
            return "Hide soft keyboard.";
        }
    };

    private static boolean checkArgumentType(Class<?>[] expectedClasses, Object... args) {
        if (args == null || args.length < expectedClasses.length)
            return false;
        for (int i = 0; i < expectedClasses.length; i++) {
            Class<?> expectedClass = expectedClasses[i];
            Object argument = args[i];
            if (!expectedClass.isInstance(argument))
                return false;
        }

        return true;
    }
}
