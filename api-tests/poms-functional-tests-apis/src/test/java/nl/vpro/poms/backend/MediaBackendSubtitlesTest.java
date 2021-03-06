package nl.vpro.poms.backend;


import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import lombok.extern.log4j.Log4j2;
import nl.vpro.api.rs.subtitles.Constants;
import nl.vpro.domain.media.AVType;
import nl.vpro.domain.media.MediaTestDataBuilder;
import nl.vpro.domain.media.update.ProgramUpdate;
import nl.vpro.domain.subtitles.*;
import nl.vpro.poms.AbstractApiMediaBackendTest;
import nl.vpro.poms.Require.Needs;
import nl.vpro.test.jupiter.AbortOnException;
import nl.vpro.util.Version;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.vpro.api.client.utils.Config.Prefix.npo_backend_api;
import static nl.vpro.testutils.Utils.waitUntil;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;


/*
 * 2018-08-15:
 * 5.9-SNAPSHOT @ dev : allemaal ok
 */
/**
 * @author Michiel Meeuwissen
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Log4j2
public class MediaBackendSubtitlesTest extends AbstractApiMediaBackendTest {

    private static final Duration ACCEPTABLE_DURATION = Duration.ofMinutes(3);

    private static String firstTitle;
    private static String updatedFirstTitle;
    private static Instant creationDate;


    @Test
    @Order(1)
    @Needs(MID)
    public void addSubtitles() {
        assumeThat(backendVersionNumber).isGreaterThanOrEqualTo(Version.of(5, 1));

        firstTitle = title;
        Subtitles subtitles = Subtitles.webvttTranslation(MID, Duration.ofMinutes(2), Locale.CHINESE,
            "WEBVTT\n" +
                "X-TIMESTAMP-MAP=MPEGTS:900000,LOCAL:00:00:00.000\n" +
                "\n" +
                "1\n" +
                "00:00:02.200 --> 00:00:04.150\n" +
                "" + title + "\n" +
                "\n" +
                "2\n" +
                "00:00:04.200 --> 00:00:08.060\n" +
                "*'k Heb een paar puntjes die ik met je wil bespreken\n" +
                "\n" +
                "3\n" +
                "00:00:08.110 --> 00:00:11.060\n" +
                "*Dat wil ik doen in jouw mobiele bakkerij\n" +
                "\n" +
                ""
        );
        backend.setSubtitles(subtitles);
    }



    @Test
    @Order(2)
    public void checkArrivedInBackend() {
        assumeThat(firstTitle).isNotNull();
        final Subtitles[] found = new Subtitles[1];
        PeekingIterator<StandaloneCue> iterator = waitUntil(ACCEPTABLE_DURATION,
            MID + "/" + Locale.CHINESE + "[0]=" + firstTitle,
            () ->  {
                found[0] = backend.getBackendRestService().getSubtitles(MID, Locale.CHINESE, SubtitlesType.TRANSLATION, true, false);
                return Iterators.peekingIterator(SubtitlesUtil.standaloneStream(found[0], false, false).iterator());
            }
            , (cpi) -> cpi != null && cpi.hasNext() && cpi.peek().getContent().equals(firstTitle));

        assertThat(iterator).toIterable().hasSize(3);

        assertThat(found[0].getCreationInstant()).isEqualTo(found[0].getLastModifiedInstant());
        creationDate = found[0].getCreationInstant();

    }


    @Test
    @Order(3)
    public void updateSubtitles() throws InterruptedException {
        assumeThat(backendVersionNumber).isGreaterThanOrEqualTo(Version.of(5, 11));
        Thread.sleep(2L);
        updatedFirstTitle = title;
        Subtitles subtitles = Subtitles.webvttTranslation(MID, Duration.ofMinutes(2), Locale.CHINESE,
            "WEBVTT\n" +
                "\n" +
                "1\n" +
                "00:00:02.200 --> 00:00:04.150\n" +
                "" + title + "\n" +
                "\n" +
                "2\n" +
                "00:00:04.200 --> 00:00:08.060\n" +
                "*'k Heb een paar puntjes die ik met je wil bespreken\n" +
                "\n" +
                "3\n" +
                "00:00:08.110 --> 00:00:11.060\n" +
                "*Dat wil ik doen in jouw mobiele bakkerij\n" +
                "\n" +
                ""
        );
        backend.setSubtitles(subtitles);
    }

    @Test
    @Order(4)
    public void checkUpdateArrived() {
        assumeThat(updatedFirstTitle).isNotNull();

        final Subtitles[] found = new Subtitles[1];
        PeekingIterator<StandaloneCue> iterator = waitUntil(ACCEPTABLE_DURATION,
            MID + "/" + Locale.CHINESE + "[0]=" + updatedFirstTitle,
            () -> {
                found[0] = backend.getBackendRestService().getSubtitles(MID, Locale.CHINESE, SubtitlesType.TRANSLATION, true, null);
                return Iterators.peekingIterator(SubtitlesUtil.standaloneStream(found[0], false, false).iterator());
            }
            , (cpi) -> cpi != null && cpi.hasNext() && cpi.peek().getContent().equals(updatedFirstTitle));

        assertThat(iterator).toIterable().hasSize(3);

        // MSE-4486
        assertThat(found[0].getCreationInstant()).isEqualTo(creationDate);
        assertThat(found[0].getCreationInstant()).isBefore(found[0].getLastModifiedInstant());
    }




    @Test
    @Order(10)

    public void webVttWithNotesWithoutCueNumbers() throws IOException {
        InputStream input = getClass().getResourceAsStream("/POMS_VPRO_4981202.vtt");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        IOUtils.copy(input, body);
        String result = given()
            .auth()
            .basic(backend.getUserName(), backend.getPassword())
            .contentType(Constants.VTT)
            .body(body.toByteArray())
            .queryParam("errors", backend.getErrors())
            .log().uri().log().parameters().log().headers()
            .when()
            .  post(CONFIG.url(npo_backend_api, "media/subtitles/" + MID + "/ar/TRANSLATION"))
            .then()
            .  log().all()
            .statusCode(202)
            .extract().asString();
        log.info(result);
    }


    @Test
    @Order(11)
    public void checkWebVttWithNotesWithoutCueNumberArrived() {

        PeekingIterator<StandaloneCue> iterator = waitUntil(ACCEPTABLE_DURATION,
            MID + "/ar has cues" ,
            () -> Iterators.peekingIterator(
                SubtitlesUtil.standaloneStream(
                    backend.getBackendRestService().getSubtitles(MID,
                        new Locale("ar"), SubtitlesType.TRANSLATION, true, null), false, false).iterator()
            )
            , (cpi) -> cpi != null && cpi.hasNext());

        assertThat(iterator).toIterable().hasSize(430);
    }


    private static String newMid;

    @Test
    @Disabled("Known to fail MSE-3836")
    @Order(20)
    public void createSubtitlesForNewClip() {

        ProgramUpdate clip = ProgramUpdate.create(MediaTestDataBuilder.clip()
            .mainTitle(title)
            .broadcasters("VPRO")
            .avType(AVType.VIDEO).build());

        newMid = backend.set(clip);


        log.info("New mid {}", newMid);

        Subtitles subtitles = Subtitles.webvttTranslation(newMid, Duration.ofMinutes(2), new Locale("ar"),
            "WEBVTT\n" +
                "\n" +
                "00:00:02.200 --> 00:00:04.150\n" +
                "" + title + "\n" +
                "\n" +
                "00:00:04.200 --> 00:00:08.060\n" +
                "*'مجلس النواب يريد المزيد من التدقيق في طلبات لجوء المثليين الجنسيين\n" +
                "\n" +
                ""
        );
        backend.setSubtitles(subtitles);
    }


    @Test
    @Order(21)
    public void checkCreateSubtitlesForNewClipCheckArrived() {
        assumeThat(newMid).isNotNull();

        PeekingIterator<StandaloneCue> iterator = waitUntil(ACCEPTABLE_DURATION,
            newMid + "/ar has cues",
            () -> Iterators.peekingIterator(
                SubtitlesUtil.standaloneStream(
                    backend.getBackendRestService().getSubtitles(newMid,
                        new Locale("ar"), SubtitlesType.TRANSLATION, true, false), false, false).iterator()
            )
            , (cpi) -> cpi != null && cpi.hasNext());

        assertThat(iterator).toIterable().hasSize(2);
    }

    @Test
    @AbortOnException.NoAbort
    @Order(100)
    public void cleanUp() {
        backend.deleteSubtitles(SubtitlesId.builder().mid(MID).language(new Locale("ar")).type(SubtitlesType.TRANSLATION).build());
        backend.deleteSubtitles(SubtitlesId.builder().mid(MID).language(Locale.CHINESE).type(SubtitlesType.TRANSLATION).build());
        if (newMid != null) {
            backend.delete(newMid);
        }
        //Subtitles subtitles = backend.getBackendRestService().getSubtitles(MID, new Locale("ar"), SubtitlesType.TRANSLATION, null);
    }

    @Test
    @AbortOnException.NoAbort
    @Order(101)
    public void checkCleanup() {
        waitUntil(ACCEPTABLE_DURATION,
            MID + " ar subtitles dissappeared",
            () -> backend.getBackendRestService().getSubtitles(MID, new Locale("ar"), SubtitlesType.TRANSLATION, null, null) == null
        );
        waitUntil(ACCEPTABLE_DURATION,
            MID + " zh subtitles dissappeared",
            () -> backend.getBackendRestService().getSubtitles(MID, Locale.CHINESE, SubtitlesType.TRANSLATION, null, null) == null
        );
        if (newMid != null) {
            waitUntil(ACCEPTABLE_DURATION,
                newMid + " ar subtitles dissappeared",
                () -> backend.getBackendRestService().getSubtitles(newMid, new Locale("ar"), SubtitlesType.TRANSLATION, null, null) == null
            );
        }
    }



    //@Test
    public void testForCamielNL() throws IOException {

        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/POW_03372714.vtt"), UTF_8);
        StringWriter writer = new StringWriter();
        IOUtils.copy(reader, writer);
        reader.close();

        Subtitles subtitles = Subtitles.webvtt("WO_VPRO_11241856", Duration.ofMinutes(0), new Locale("nl"), writer.toString());
        subtitles.setType(SubtitlesType.CAPTION);


        backend.setSubtitles(subtitles);
    }

    //@Test
    public void test99ForCamielAR() throws IOException {

        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/POMS_VPRO_4981202.vtt"), UTF_8);

        StringWriter writer = new StringWriter();
        IOUtils.copy(reader, writer);
        reader.close();

        Subtitles subtitles = Subtitles.webvttTranslation("POMS_KRO_3852926", Duration.ofMinutes(0), new Locale("ar"), writer.toString());


        Subtitles corrected = Subtitles.from(subtitles.getId(), SubtitlesUtil.fillCueNumber(SubtitlesUtil.parse(subtitles, false).getCues()).iterator());


        backend.setSubtitles(corrected);
    }

    //@Test
    public void test99ForCamielNL2() throws IOException {

        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/POMS_VPRO_4959361.vtt"), UTF_8);

        StringWriter writer = new StringWriter();
        IOUtils.copy(reader, writer);
        reader.close();

        Subtitles subtitles = Subtitles.webvtt("POMS_KRO_3852926", Duration.ofMinutes(0), new Locale("nl"), writer.toString());

        Subtitles corrected = Subtitles.from(subtitles.getId(), SubtitlesUtil.fillCueNumber(SubtitlesUtil.parse(subtitles, false).getCues()).iterator());


        backend.setSubtitles(corrected);
    }

    //@Test
    public void test99deleteCaption() {

        backend.deleteSubtitles(SubtitlesId.builder().language(new Locale("ar")).type(SubtitlesType.CAPTION).mid("POMS_KRO_3852926").build());
    }

}
