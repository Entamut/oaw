package es.inteco.rastreador2.pdf;

import es.inteco.common.Constants;
import es.inteco.common.logging.Logger;
import es.inteco.common.properties.PropertiesManager;
import es.inteco.intav.datos.AnalisisDatos;
import es.inteco.intav.datos.CSSDTO;
import es.inteco.intav.persistence.Analysis;
import es.inteco.plugin.dao.DataBaseManager;
import es.inteco.rastreador2.actionform.semillas.SemillaForm;
import es.inteco.rastreador2.dao.observatorio.ObservatorioDAO;
import es.inteco.rastreador2.dao.rastreo.FulFilledCrawling;
import es.inteco.rastreador2.dao.rastreo.RastreoDAO;
import es.inteco.rastreador2.dao.semilla.SemillaDAO;
import es.inteco.rastreador2.pdf.builder.AnonymousResultExportPdfUNE2012;
import es.inteco.rastreador2.pdf.utils.PDFUtils;
import es.inteco.rastreador2.pdf.utils.PrimaryExportPdfUtils;
import es.inteco.rastreador2.pdf.utils.ZipUtils;
import es.inteco.utils.FileUtils;
import es.inteco.utils.MailUtils;
import org.apache.struts.util.PropertyMessageResources;
import org.apache.tika.io.FilenameUtils;

import java.io.*;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;

import static es.inteco.common.Constants.CRAWLER_PROPERTIES;

/**
 * Hilo para generar los pdfs de un observatorio de forma asíncrona
 */
public class PdfGeneratorThread extends Thread {

    private final long idObservatory;
    private final long idObservatoryExecution;
    private final List<FulFilledCrawling> fulfilledCrawlings;
    private final String email;

    public PdfGeneratorThread(final long idObservatory, final long idObservatoryExecution, final List<FulFilledCrawling> fulfilledCrawlings, final String email) {
        super("PdfGeneratorThread");
        this.idObservatory = idObservatory;
        this.fulfilledCrawlings = fulfilledCrawlings;
        this.idObservatoryExecution = idObservatoryExecution;
        this.email = email;
    }

    @Override
    public final void run() {
        for (FulFilledCrawling fulfilledCrawling : fulfilledCrawlings) {
            buildPdf(fulfilledCrawling.getId(), fulfilledCrawling.getIdCrawling());
        }
        final PropertiesManager pmgr = new PropertiesManager();
        final String alertFromAddress = pmgr.getValue(CRAWLER_PROPERTIES, "alert.from.address");
        final String alertFromName = pmgr.getValue(CRAWLER_PROPERTIES, "alert.from.name");
        MailUtils.sendSimpleMail(alertFromAddress, alertFromName, Collections.singletonList(email), "Generación de informes completado", "El proceso de generación de informes ha finalizado. Para descargar los informes vuelva a ejecutar la acción");

    }

    private void buildPdf(final long idRastreoRealizado, final long idRastreo) {
        try (Connection c = DataBaseManager.getConnection()) {
            final SemillaForm seed = SemillaDAO.getSeedById(c, RastreoDAO.getIdSeedByIdRastreo(c, idRastreo));
            final File pdfFile = getReportFile(seed);
            // Si el pdf no ha sido creado lo creamos
            if (!pdfFile.exists()) {
                final List<Long> evaluationIds = AnalisisDatos.getEvaluationIdsFromRastreoRealizado(idRastreoRealizado);
                final List<Long> previousEvaluationIds;
                if (evaluationIds != null && !evaluationIds.isEmpty()) {
                    final es.ctic.rastreador2.observatorio.ObservatoryManager observatoryManager = new es.ctic.rastreador2.observatorio.ObservatoryManager();
                    previousEvaluationIds = AnalisisDatos.getEvaluationIdsFromRastreoRealizado(observatoryManager.getPreviousIdRastreoRealizadoFromIdRastreoAndIdObservatoryExecution(idRastreo, ObservatorioDAO.getPreviousObservatoryExecution(c, idObservatoryExecution)));
                    final long observatoryType = ObservatorioDAO.getObservatoryForm(c, idObservatory).getTipo();

                    PrimaryExportPdfUtils.exportToPdf(new AnonymousResultExportPdfUNE2012(), idRastreoRealizado, evaluationIds, previousEvaluationIds, PropertyMessageResources.getMessageResources("ApplicationResources"), null, pdfFile.getPath(), seed.getNombre(), "", idObservatoryExecution, observatoryType);
                    writeSourceFiles(c, evaluationIds, pdfFile);

                    ZipUtils.generateZipFile(pdfFile.getParentFile().toString() + "/sources", pdfFile.getParentFile().toString() + "/sources.zip", true);
                    FileUtils.deleteDir(new File(pdfFile.getParent() + File.separator + "sources"));
                    FileUtils.deleteDir(new File(pdfFile.getParent() + File.separator + "temp"));
                }
            }
        } catch (Exception e) {
            Logger.putLog("Exception: ", PdfGeneratorThread.class, Logger.LOG_LEVEL_ERROR, e);
        }
    }

    private void writeSourceFiles(final Connection c, final List<Long> evaluationIds, final File pdfFile) throws IOException {
        int index = 1;
        for (Long evaluationId : evaluationIds) {
            final File pageSourcesDirectory = new File(pdfFile.getParentFile(), "sources/" + index);
            if (!pageSourcesDirectory.mkdirs()) {
                Logger.putLog("No se ha podido crear el directorio sources - " + pageSourcesDirectory.getAbsolutePath(), PdfGeneratorThread.class, Logger.LOG_LEVEL_ERROR);
            }
            try (PrintWriter fw = new PrintWriter(new FileWriter(new File(pageSourcesDirectory, "references.txt"), true))) {
                final Analysis analysis = AnalisisDatos.getAnalisisFromId(c, evaluationId);
                final File htmlTempFile = File.createTempFile("oaw_", "_" + getURLFileName(analysis.getUrl(), "html.html"), pageSourcesDirectory);
                fw.println(writeTempFile(htmlTempFile, analysis.getSource(), analysis.getUrl()));
                final List<CSSDTO> cssResourcesFromEvaluation = AnalisisDatos.getCSSResourcesFromEvaluation(evaluationId);
                for (CSSDTO cssdto : cssResourcesFromEvaluation) {
                    final File stylesheetTempFile = File.createTempFile("oaw_", "_" + getURLFileName(cssdto.getUrl(), "css.css"), pageSourcesDirectory);
                    fw.println(writeTempFile(stylesheetTempFile, cssdto.getCodigo(), cssdto.getUrl()));
                }
                index++;
                fw.flush();
            }
        }
    }

    private String writeTempFile(final File tempFile, final String source, final String url) throws FileNotFoundException {
        try (PrintWriter writer = new PrintWriter(tempFile)) {
            writer.print(source);
            writer.flush();
        }
        return tempFile.getName() + " --> " + url;
    }

    private File getReportFile(final SemillaForm seed) {
        final PropertiesManager pmgr = new PropertiesManager();
        String dependOn = PDFUtils.formatSeedName(seed.getDependencia());
        if (dependOn == null || dependOn.isEmpty()) {
            dependOn = Constants.NO_DEPENDENCE;
        }
        final String path = pmgr.getValue(CRAWLER_PROPERTIES, "path.inteco.exports.observatory.intav") + idObservatory + File.separator + idObservatoryExecution + File.separator + dependOn + File.separator + PDFUtils.formatSeedName(seed.getNombre());
        return new File(path + File.separator + PDFUtils.formatSeedName(seed.getNombre()) + ".pdf");
    }

    private String getURLFileName(final String url, final String defaultValue) {
        final String fileName = FilenameUtils.getName(FilenameUtils.normalize(url));
        return fileName.isEmpty() ? defaultValue : fileName;
    }
}
