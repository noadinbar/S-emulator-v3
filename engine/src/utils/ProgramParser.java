package utils;
import structure.program.SProgram;

public interface ProgramParser {
    ParseResult parseProgramFile(String filePath);
    String validateProgram(SProgram program);
}