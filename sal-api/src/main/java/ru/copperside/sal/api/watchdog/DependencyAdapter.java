package ru.copperside.sal.api.watchdog;

import java.util.ArrayList;
import java.util.List;

/** C# origin: {@code TCB.SAL.Common.WatchDog.DependencyAdapter} */
public class DependencyAdapter {

    private String namePattern;
    private final List<String> adapterNames = new ArrayList<>();

    public String getNamePattern() { return namePattern; }
    public void setNamePattern(String namePattern) { this.namePattern = namePattern; }
    public List<String> getAdapterNames() { return adapterNames; }
}
