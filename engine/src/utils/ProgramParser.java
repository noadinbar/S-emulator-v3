package utils;
import xmlmodel.SProgram;

public interface ProgramParser {
    ParseResult parseProgramFile(String filePath);
    String validateProgram(SProgram program);
}