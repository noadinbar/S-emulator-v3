package application.listeners;

import application.execution.ExecutionTaskManager;
import application.functions.FunctionManager;
import application.history.HistoryManager;
import application.programs.ProgramManager;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import users.UserManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebListener
public class AppContextListener implements ServletContextListener {
    public static final String ATTR_USERS     = "userManager";
    public static final String ATTR_PROGRAMS  = "programsRegistry";
    public static final String ATTR_FUNCTIONS = "functionsRegistry";
    public static final String ATTR_HISTORY   = "historyManager";
    public static final String ATTR_FUNCTION_NAMES = "functionNameToUserString";
    public static final String ATTR_USER_OUT_OF_CREDITS = "userOutOfCreditsMap";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        ctx.setAttribute(ATTR_USERS,     new UserManager());
        ctx.setAttribute(ATTR_PROGRAMS,  new ProgramManager());
        ctx.setAttribute(ATTR_FUNCTIONS, new FunctionManager());
        ctx.setAttribute(ATTR_HISTORY,   new HistoryManager());
        ctx.setAttribute(ATTR_FUNCTION_NAMES, new ConcurrentHashMap<String, String>());
        ctx.setAttribute(ATTR_USER_OUT_OF_CREDITS, new ConcurrentHashMap<String, Boolean>());
    }

    public static UserManager getUsers(ServletContext ctx) {
        return (UserManager) ctx.getAttribute(ATTR_USERS);
    }

    public static ProgramManager getPrograms(ServletContext ctx) {
        return (ProgramManager) ctx.getAttribute(ATTR_PROGRAMS);
    }

    public static FunctionManager getFunctions(ServletContext ctx) {
        return (FunctionManager) ctx.getAttribute(ATTR_FUNCTIONS);
    }

    public static HistoryManager getHistory(ServletContext ctx) {
        return (HistoryManager) ctx.getAttribute(ATTR_HISTORY);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getFunctionNames(ServletContext ctx) {
        return (Map<String, String>) ctx.getAttribute(ATTR_FUNCTION_NAMES);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Boolean> getUserOutOfCreditsMap(ServletContext ctx) {
        return (Map<String, Boolean>) ctx.getAttribute(ATTR_USER_OUT_OF_CREDITS);
    }

    /** Called at the start of a new run/debug attempt to reset the flag for this user. */
    public static void clearUserOutOfCredits(ServletContext ctx, String username) {
        if (username == null) {
            return;
        }
        getUserOutOfCreditsMap(ctx).remove(username);
    }

    /** Called when we detect the user cannot afford more cycles mid-run / mid-debug. */
    public static void markUserOutOfCredits(ServletContext ctx, String username) {
        if (username == null) {
            return;
        }
        getUserOutOfCreditsMap(ctx).put(username, Boolean.TRUE);
    }

    /** Ask "did this user run out of credits in the current/last session?" */
    public static boolean isUserOutOfCredits(ServletContext ctx, String username) {
        if (username == null) {
            return false;
        }
        Boolean v = getUserOutOfCreditsMap(ctx).get(username);
        return v != null && v.booleanValue();
    }


    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ExecutionTaskManager.shutdown();
    }
}
