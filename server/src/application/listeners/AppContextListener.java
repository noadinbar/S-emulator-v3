// server/src/application/listeners/AppContextListener.java
package application.listeners;

import application.functions.FunctionManager;
import application.programs.ProgramManager;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import users.UserManager;

@WebListener
public class AppContextListener implements ServletContextListener {
    // Context attribute keys (keep existing names)
    public static final String ATTR_USERS     = "userManager";
    public static final String ATTR_PROGRAMS  = "programsRegistry";
    public static final String ATTR_FUNCTIONS = "functionsRegistry";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        ctx.setAttribute(ATTR_USERS,    new UserManager());
        ctx.setAttribute(ATTR_PROGRAMS, new ProgramManager());
        ctx.setAttribute(ATTR_FUNCTIONS, new FunctionManager());
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
}
