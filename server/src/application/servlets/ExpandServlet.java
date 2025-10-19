package application.servlets;

import api.DisplayAPI;
import display.ExpandDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static utils.Constants.ATTR_DISPLAY_API;
import static utils.Constants.QP_DEGREE;
import static utils.Constants.API_EXPAND;
import static utils.ServletUtils.writeJson;
import static utils.ServletUtils.writeJsonError;

import java.io.IOException;

@WebServlet(name = "ExpandServlet", urlPatterns = {API_EXPAND})
public class ExpandServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Object obj = getServletContext().getAttribute(ATTR_DISPLAY_API);
        if (!(obj instanceof DisplayAPI)) {
            writeJsonError(resp, HttpServletResponse.SC_CONFLICT, "No loaded program.");
            return;
        }
        DisplayAPI rootDisplay = (DisplayAPI) obj;
        String degreeStr = req.getParameter(QP_DEGREE);
        int degree;
        try {
            degree = Integer.parseInt(degreeStr);
        } catch (Exception e) {
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing/invalid 'degree' query parameter.");
            return;
        }
        String fn = req.getParameter("function");
        try {
            ExpandDTO out;
            if (fn == null || fn.isBlank()) {
                // PROGRAM
                out = rootDisplay.expand(degree);
            } else {
                // FUNCTION
                DisplayAPI fnDisplay = rootDisplay.functionDisplaysByUserString().get(fn);
                if (fnDisplay == null) {
                    writeJsonError(resp, HttpServletResponse.SC_NOT_FOUND,
                            "Function not found: " + fn);
                    return;
                }
                out = fnDisplay.expand(degree);
            }
            writeJson(resp, HttpServletResponse.SC_OK, out);
        } catch (Exception e) {
            String msg = e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "" : e.getMessage());
            writeJsonError(resp, HttpServletResponse.SC_BAD_REQUEST, msg);
        }
    }
}

