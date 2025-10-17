// server/src/application/listeners/AppContextListener.java
package application.listeners;

import application.programs.ProgramManager;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import users.UserManager;

@WebListener
public class AppContextListener implements ServletContextListener {
    public static final String ATTR_USERS = "userManager";
    public static final String ATTR_PROGRAMS = "programsRegistry";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        ctx.setAttribute(ATTR_USERS, new UserManager());
        ctx.setAttribute(ATTR_PROGRAMS, new ProgramManager());
    }

    public static UserManager getUsers(ServletContext ctx) {
        return (UserManager) ctx.getAttribute(ATTR_USERS);
    }
}
