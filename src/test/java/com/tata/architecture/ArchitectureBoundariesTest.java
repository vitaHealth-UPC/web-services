package com.tata.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ArchitectureBoundariesTest {
    private final Path sources = Path.of("src/main/java/com/tata");

    @Test
    void domainDoesNotDependOnPersistenceOrFrameworks() throws Exception {
        var violations = new ArrayList<String>();
        try (var files = Files.walk(sources)) {
            for (var path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (!path.toString().contains("domain")) continue;
                var source = Files.readString(path);
                if (Pattern.compile("import (jakarta.persistence|org.hibernate|org.springframework)\\.").matcher(source).find())
                    violations.add(path.toString());
            }
        }
        assertTrue(violations.isEmpty(), "Domain depends on infrastructure: " + violations);
    }

    @Test
    void contextsDoNotImportAnotherContextsPrivateImplementation() throws Exception {
        var violations = new ArrayList<String>();
        var imports = Pattern.compile("import com\\.tata\\.(\\w+)\\.(application\\.internal|infrastructure|domain\\.repositories|domain\\.services)\\.");
        try (var files = Files.walk(sources)) {
            for (var path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                var context = sources.relativize(path).getName(0).toString();
                var matcher = imports.matcher(Files.readString(path));
                while (matcher.find()) {
                    if (!context.equals(matcher.group(1)) && !matcher.group(1).equals("shared"))
                        violations.add(path + " -> " + matcher.group());
                }
            }
        }
        assertTrue(violations.isEmpty(), "Private cross-context dependencies: " + violations);
    }

    @Test
    void packageDeclarationMatchesTheFolder() throws Exception {
        var violations = new ArrayList<String>();
        var declaration = Pattern.compile("package ([\\w.]+);");
        try (var files = Files.walk(sources)) {
            for (var path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                var parent = sources.relativize(path).getParent();
                var expected = "com.tata" + (parent == null ? "" : "." + parent.toString().replace('\\', '.').replace('/', '.'));
                var matcher = declaration.matcher(Files.readString(path));
                if (!matcher.find() || !expected.equals(matcher.group(1))) violations.add(path.toString());
            }
        }
        assertTrue(violations.isEmpty(), "Packages outside their folders: " + violations);
    }
    @Test
    void restControllersUsePublicApplicationContractsAndDoNotOwnTransactions() throws Exception {
        var violations = new ArrayList<String>();
        try (var files = Files.walk(sources)) {
            for (var path : files.filter(p -> p.toString().endsWith("Controller.java")).toList()) {
                var source = Files.readString(path);
                if (Pattern.compile("com\\.tata\\.\\w+\\.application\\.internal\\.").matcher(source).find()
                        || source.contains("@Transactional") || source.contains("@org.springframework.transaction.annotation.Transactional"))
                    violations.add(path.toString());
            }
        }
        assertTrue(violations.isEmpty(), "REST controllers bypass application contracts: " + violations);
    }
}
