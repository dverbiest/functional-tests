package nl.vpro.poms.npoapi;

import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.*;

import com.google.common.collect.Sets;

import nl.vpro.poms.AbstractApiTest;
import nl.vpro.test.util.jackson2.Jackson2TestUtil;
import nl.vpro.util.IntegerVersion;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
@Log4j2
public abstract class AbstractSearchTest<T, S> extends AbstractApiTest {
    private final Map<Pattern, Function<S, Boolean>> TESTERS = new HashMap<>();
    private static final Map<String, AtomicInteger> USED = new HashMap<>();
    private static final Set<String> AVAILABLE = new HashSet<>();
    private final Map<Pattern, Supplier<Boolean>> ASSUMERS =  new HashMap<>();

    Function<S, Boolean> tester;

    void  addTester(IntegerVersion minVersion, String pattern, Consumer<S> consumer) {
        if (minVersion == null || apiVersionNumber.isNotBefore(minVersion)) {
            addTester(pattern, (s) -> {
                consumer.accept(s);
                return true;
            });
        }
    }

     void  addTester(String pattern, Consumer<S> consumer) {
         addTester(null, pattern, consumer);
     }

    private void addTester(String pattern, Function<S, Boolean> consumer) {
        Pattern p = Pattern.compile(pattern);
        TESTERS.put(p, consumer);
        AVAILABLE.add(p.pattern());
    }


    void addAssumer(String pattern, Supplier<Boolean> consumer) {
        ASSUMERS.put(Pattern.compile(pattern), consumer);
    }

    @BeforeAll
    public static void clean() {
        USED.clear();
        AVAILABLE.clear();
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) {
        String[] split = testInfo.getDisplayName().split("[, ]", 3);
        String name;
        if (split.length > 1) {
            String[] kv = split[1].split("=", 2);
            name = kv.length > 1 ? kv[1] : kv[0];
        } else {
            name = testInfo.getDisplayName();
        }
        for (Map.Entry<Pattern, Supplier<Boolean>> e : ASSUMERS.entrySet()) {
            if (e.getKey().matcher(name).matches()) {
                assumeTrue(e.getValue().get(), "Skipping in " + this + " because of " + e);
            }
        }


        final List<Function<S, Boolean>> result = new ArrayList<>();
        for (Map.Entry<Pattern, Function<S, Boolean>> e : TESTERS.entrySet()) {
            if (e.getKey().matcher(name).matches()) {
                log.info("matched {}", e.getValue());
                result.add(e.getValue());
                AtomicInteger atomicInteger = USED.computeIfAbsent(e.getKey().pattern(), (k) -> new AtomicInteger(0));
                atomicInteger.incrementAndGet();
            }
        }
        final boolean doLog = ! result.isEmpty();
        if (result.isEmpty()) {
            result.add((s) -> {
                log.debug("No predicate defined for " + name);
                return true;
            });
        }
        tester = s -> {
            boolean bool = true;
            for (Function<S, Boolean> tester1 : result) {
                if (doLog) {
                    log.info("USING  TESTER " + tester1 + " for " + name);
                }
                bool &= tester1.apply(s);

            }
            return bool;

        };

    }

    protected void setupClient(MediaType accept) {
        clients.setAccept(accept);
        clients.setContentType(accept);
    }

    @AfterAll
    public static void shutdown() {
        Sets.SetView<String> difference = Sets.difference(AVAILABLE, USED.keySet());
        if (! difference.isEmpty()) {
            //log.error("Not all testers were used: " + difference);
            Assertions.fail("Not all testers were used: " + difference + " available: " + AVAILABLE.size() + " used: " + USED.size());
        }
        USED.entrySet().stream()
            .map((e) -> e.getKey() + " was used " + e.getValue().intValue() + " times")
            .forEach(log::info);

    }


    private static Supplier<Boolean> minVersion(final IntegerVersion minVersion) {
        return new Supplier<>() {
            @Override
            public Boolean get() {
                return apiVersionNumber.isNotBefore(minVersion);
            }

            @Override
            public String toString() {
                return "" + apiVersionNumber + " < " + minVersion;
            }
        };
    }
    static Supplier<Boolean> minVersion(int... parts) {
        return minVersion(IntegerVersion.of(parts));
    }


    <U> U test(String name, U object) throws Exception {
        return Jackson2TestUtil.roundTrip(object);

    }

}
