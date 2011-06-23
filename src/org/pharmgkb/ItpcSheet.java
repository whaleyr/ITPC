package org.pharmgkb;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import util.ExcelUtils;
import util.ItpcUtils;
import util.PoiWorksheetIterator;
import util.Value;

import java.io.*;
import java.util.Iterator;
import java.util.List;


/**
 * Created by IntelliJ IDEA. User: whaleyr Date: Jun 18, 2010 Time: 10:07:11 AM To change this template use File |
 * Settings | File Templates.
 */
public class ItpcSheet implements Iterator {
  public static final String SHEET_NAME = "Combined_Data";
  private static final Logger sf_logger = Logger.getLogger(ItpcSheet.class);

  private File inputFile = null;

  private Sheet m_dataSheet = null;
  private int m_rowIndex = -1;

  private CellStyle styleTitle = null, styleDescr = null;
  private CellStyle styleHighlight = null;

  protected int subjectId = -1;
  protected int projectSiteIdx = -1;
  protected int ageIdx = -1;
  protected int genderIdx = -1;
  protected int raceIdx = -1;
  protected int menoStatusIdx = -1;
  protected int metastaticIdx = -1;
  protected int erStatusIdx = -1;
  protected int durationIdx = -1;
  protected int tamoxDoseIdx = -1;
  protected int tumorSourceIdx = -1;
  protected int bloodSourceIdx = -1;
  protected int priorHistoryIdx = -1;
  protected int priorSitesIdx = -1;
  protected int priorDcisIdx = -1;
  protected int chemoIdx = -1;
  protected int hormoneIdx = -1;
  protected int systemicTherIdx = -1;
  protected int followupIdx = -1;
  protected int timeBtwSurgTamoxIdx = -1;
  protected int firstAdjEndoTherIdx = -1;
  protected int genoSourceIdx1 = -1;
  protected int genoSourceIdx2 = -1;
  protected int genoSourceIdx3 = -1;
  protected int projectNotesIdx = -1;
  protected int tumorDimensionIdx = -1;
  protected int additionalCancerIdx = -1;
  protected int addCxIpsilateralIdx = -1;
  protected int addCxDistantRecurIdx = -1;
  protected int addCxContralateralIdx = -1;
  protected int addCxSecondInvasiveIdx = -1;
  protected int addCxLastEvalIdx = -1;
  protected int daysDiagToDeathIdx = -1;
  protected int patientDiedIdx = -1;

  protected int fluoxetineCol = -1;
  protected int paroxetineCol = -1;
  protected int quinidienCol = -1;
  protected int buproprionCol = -1;
  protected int duloxetineCol = -1;
  protected int cimetidineCol = -1;
  protected int sertralineCol = -1;
  protected int citalopramCol = -1;

  protected int rs4986774idx = -1;
  protected int rs1065852idx = -1;
  protected int rs3892097idx = -1;
  protected int star5idx = -1;
  protected int rs5030655idx = -1;
  protected int rs16947idx = -1;
  protected int rs28371706idx = -1;
  protected int rs28371725idx = -1;

  protected int amplichipidx = -1;
  protected int otherGenoIdx = -1;

  protected int allele1finalIdx = -1;
  protected int allele2finalIdx = -1;
  protected int genotypeIdx = -1;
  protected int genoMetabStatusIdx = -1;
  protected int weakIdx = -1;
  protected int potentIdx = -1;
  protected int metabStatusIdx = -1;
  protected int includeCrit1Idx = -1;
  protected int includeCrit2Idx = -1;
  protected int includeCrit3Idx = -1;
  protected int scoreIdx = -1;
  protected int exclude1Idx = -1;
  protected int exclude2Idx = -1;
  protected int exclude3Idx = -1;

  protected int incAgeIdx = -1;
  protected int incNonmetaIdx = -1;
  protected int incPriorHistIdx = -1;
  protected int incErPosIdx = -1;
  protected int incSysTherIdx = -1;
  protected int incAdjTamoxIdx = -1;
  protected int incDurationIdx = -1;
  protected int incTamoxDoseIdx = -1;
  protected int incChemoIdx = -1;
  protected int incHormoneIdx = -1;
  protected int incDnaCollectionIdx = -1;
  protected int incFollowupIdx = -1;
  protected int incGenoDataAvailIdx = -1;

  private PoiWorksheetIterator m_sampleIterator = null;

  /**
   * Constructor for an ITPC data file
   * <br/>
   * Expectations for <code>file</code> parameter:
   * <ol>
   * <li>file is an Excel .XLS formatted spreadsheet</li>
   * <li>there is a sheet in the file called "Combined_Data"</li>
   * <li>the sheet has the first row as column headers</li>
   * <li>the sheet has the second row as column legends</li>
   * </ol>
   * After this has been initialized, samples can be gathered by using the <code>getSampleIterator</code> method
   * @param file an Excel .XLS file
   * @param doHighlighting highlight changed cells in the output file
   * @throws Exception can occur from file I/O
   */
  public ItpcSheet(File file, boolean doHighlighting) throws Exception {
    if (file == null || !(file.getName().endsWith(".xls") || file.getName().endsWith(".xlsx"))) {
      throw new Exception("File not in right format: " + file);
    }

    inputFile = file;
    InputStream inputFileStream = null;
    sf_logger.info("Using input file: " + inputFile);

    try {
      inputFileStream = new FileInputStream(inputFile);
      Workbook inputWorkbook = WorkbookFactory.create(inputFileStream);
      Sheet inputSheet = inputWorkbook.getSheet(SHEET_NAME);
      if (inputSheet == null) {
        throw new Exception("Cannot find worksheet named " + SHEET_NAME);
      }

      m_dataSheet = inputSheet;
      if (doHighlighting) {
        doHighlighting();
      }

      parseColumnIndexes();

      PoiWorksheetIterator sampleIterator = new PoiWorksheetIterator(m_dataSheet);
      setSampleIterator(sampleIterator);
      skipNext(); // skip header row
      skipNext(); // skip legend row
    }
    catch (Exception ex) {
      throw new Exception("Error initializing ITPC Sheet", ex);
    }
    finally {
      if (inputFileStream != null) {
        IOUtils.closeQuietly(inputFileStream);
      }
    }
  }

  protected void parseColumnIndexes() throws Exception {
    if (sf_logger.isDebugEnabled()) {
      sf_logger.debug("Parsing column indexes and headings");
    }

    Row headerRow = m_dataSheet.getRow(0);
    Iterator<Cell> headerCells = headerRow.cellIterator();

    while(headerCells.hasNext()) {
      Cell headerCell = headerCells.next();
      String header = headerCell.getStringCellValue();
      int idx = headerCell.getColumnIndex();

      if (StringUtils.isNotEmpty(header)) {
        header = header.trim().toLowerCase();
      }
      if (header.contains("subject id")) {
        subjectId = idx;
      } else if (header.equalsIgnoreCase("project site")) {
        projectSiteIdx = idx;
      } else if (header.contains("gender")) {
        genderIdx = idx;
      } else if (header.contains("age at diagnosis")) {
        ageIdx = idx;
      } else if (header.contains("race") && header.contains("omb")) {
        raceIdx = idx;
      } else if (header.contains("metastatic disease")) {
        metastaticIdx = idx;
      } else if (header.contains("maximum dimension of tumor")) {
        tumorDimensionIdx = idx;
      } else if (header.contains("menopause status at diagnosis")) {
        menoStatusIdx = idx;
      } else if (header.equals("estrogen receptor")) {
        erStatusIdx = idx;
      } else if (header.contains("intended tamoxifen duration")) {
        durationIdx = idx;
      } else if (header.contains("intended tamoxifen dose")) {
        tamoxDoseIdx = idx;
      } else if (header.contains("if tumor or tissue was dna source")) {
        tumorSourceIdx = idx;
      } else if (header.contains("blood or buccal cells")) {
        bloodSourceIdx = idx;
      } else if (header.contains("prior history of cancer")) {
        priorHistoryIdx = idx;
      } else if (header.contains("sites of prior cancer")) {
        priorSitesIdx = idx;
      } else if (header.contains("prior invasive breast cancer or dcis")) {
        priorDcisIdx = idx;
      } else if (header.equalsIgnoreCase("chemotherapy")) {
        chemoIdx = idx;
      } else if (header.contains("additional hormone or other treatment after breast surgery?")) {
        hormoneIdx = idx;
      } else if (header.contains("systemic therapy prior to surgery?")) {
        systemicTherIdx = idx;
      } else if (header.contains("annual physical exam after breast cancer surgery")) {
        followupIdx = idx;
      } else if (header.contains("time between definitive breast cancer surgery")) {
        timeBtwSurgTamoxIdx = idx;
      } else if (header.contains("first adjuvant endocrine therapy")) {
        firstAdjEndoTherIdx = idx;
      } else if (header.contains("project notes")) {
        projectNotesIdx = idx;
      } else if (header.equalsIgnoreCase("other genotyping")) {
        otherGenoIdx = idx;
      } else if (header.contains("rs4986774")) {
        if (!header.contains("source")) {
          rs4986774idx = idx;
        }
        else {
          genoSourceIdx1 = idx;
        }
      } else if (header.contains("rs1065852")) {
        if (!header.contains("source")) {
          rs1065852idx = idx;
        }
        else {
          genoSourceIdx2 = idx;
        }
      } else if (header.contains("rs3892097")) {
        if (!header.contains("source")) {
          rs3892097idx = idx;
        }
        else {
          genoSourceIdx3 = idx;
        }
      } else if (header.contains("rs5030655") && !header.contains("source")) {
        rs5030655idx = idx;
      } else if (header.contains("rs16947") && !header.contains("source")) {
        rs16947idx = idx;
      } else if (header.contains("rs28371706") && !header.contains("source")) {
        rs28371706idx = idx;
      } else if (header.contains("rs28371725") && !header.contains("source")) {
        rs28371725idx = idx;
      } else if (header.contains("cyp2d6 *5") && !header.contains("source")) {
        star5idx = idx;
      } else if (header.contains("fluoxetine")) {
        fluoxetineCol = idx;
      } else if (header.contains("paroxetine")) {
        paroxetineCol = idx;
      } else if (header.contains("quinidine")) {
        quinidienCol = idx;
      } else if (header.contains("buproprion")) {
        buproprionCol = idx;
      } else if (header.contains("duloxetine")) {
        duloxetineCol = idx;
      } else if (header.contains("cimetidine")) {
        cimetidineCol = idx;
      } else if (header.contains("sertraline")) {
        sertralineCol = idx;
      } else if(header.equals("citalopram")) {
        citalopramCol = idx;
      } else if (header.contains("amplichip call")) {
        amplichipidx = idx;
      } else if (header.equalsIgnoreCase("Additional cancer?")) {
        additionalCancerIdx = idx;
      } else if (header.contains("time from diagnosis to ipsilateral local or regional recurrence")) {
        addCxIpsilateralIdx = idx;
      } else if (header.contains("time from diagnosis to distant recurrence")) {
        addCxDistantRecurIdx = idx;
      } else if (header.contains("time from diagnosis to contralateral breast cancer")) {
        addCxContralateralIdx = idx;
      } else if (header.contains("time from diagnosis to second primary invasive cancer")) {
        addCxSecondInvasiveIdx = idx;
      } else if (header.contains("time from diagnosis to date of last disease evaluation")) {
        addCxLastEvalIdx = idx;
      } else if (header.equalsIgnoreCase("Time from diagnosis until death if the patient has died")) {
        daysDiagToDeathIdx = idx;
      } else if (header.equalsIgnoreCase("Has the patient died?")) {
        patientDiedIdx = idx;
      }
    }

    // new columns to add to the end of the template
    int startPgkbColsIdx = projectNotesIdx+1;
    allele1finalIdx = startPgkbColsIdx      + 0;
    allele2finalIdx = startPgkbColsIdx      + 1;
    genotypeIdx = startPgkbColsIdx          + 2;
    genoMetabStatusIdx = startPgkbColsIdx   + 3;
    weakIdx = startPgkbColsIdx              + 4;
    potentIdx = startPgkbColsIdx            + 5;
    scoreIdx = startPgkbColsIdx             + 6;
    metabStatusIdx = startPgkbColsIdx       + 7;

    incAgeIdx = startPgkbColsIdx            + 8;
    incNonmetaIdx = startPgkbColsIdx        + 9;
    incPriorHistIdx = startPgkbColsIdx      + 10;
    incErPosIdx = startPgkbColsIdx          + 11;
    incSysTherIdx = startPgkbColsIdx        + 12;
    incAdjTamoxIdx = startPgkbColsIdx       + 13;
    incDurationIdx = startPgkbColsIdx       + 14;
    incTamoxDoseIdx = startPgkbColsIdx      + 15;
    incChemoIdx = startPgkbColsIdx          + 16;
    incHormoneIdx = startPgkbColsIdx        + 17;
    incDnaCollectionIdx = startPgkbColsIdx  + 18;
    incFollowupIdx = startPgkbColsIdx       + 19;
    incGenoDataAvailIdx = startPgkbColsIdx  + 20;
    // skip for "other genotyping"

    exclude1Idx = startPgkbColsIdx          + 22;
    exclude2Idx = startPgkbColsIdx          + 23;
    exclude3Idx = startPgkbColsIdx          + 24;

    includeCrit1Idx = startPgkbColsIdx      + 25;
    includeCrit2Idx = startPgkbColsIdx      + 26;
    includeCrit3Idx = startPgkbColsIdx      + 27;

    writeCellTitles(headerRow);
    styleCells(headerRow, startPgkbColsIdx, headerRow.getCell(0).getCellStyle());

    // write the description row
    Row descrRow = m_dataSheet.getRow(1);
    writeCellDescr(descrRow);
    styleCells(descrRow, startPgkbColsIdx, descrRow.getCell(0).getCellStyle());
  }

  private void writeCellTitles(Row headerRow) {
    ExcelUtils.writeCell(headerRow, allele1finalIdx, "CYP2D6 Allele 1 (Final)");
    ExcelUtils.writeCell(headerRow, allele2finalIdx, "CYP2D6 Allele 2 (Final)");
    ExcelUtils.writeCell(headerRow, genotypeIdx, "CYP2D6 Genotype (PharmGKB)");
    ExcelUtils.writeCell(headerRow, genoMetabStatusIdx, "Metabolizer Status based on Genotypes only (PharmGKB)");
    ExcelUtils.writeCell(headerRow, weakIdx, "Weak Drug (PharmGKB)");
    ExcelUtils.writeCell(headerRow, potentIdx, "Potent Drug (PharmGKB)");
    ExcelUtils.writeCell(headerRow, scoreIdx, "Drug and CYP2D6 Genotype Score");
    ExcelUtils.writeCell(headerRow, metabStatusIdx, "Metabolizer Status based on Drug and CYP2D6 Genotypes (PharmGKB)");

    ExcelUtils.writeCell(headerRow, incAgeIdx, "Inc 1\nPostmenopausal");
    ExcelUtils.writeCell(headerRow, incNonmetaIdx, "Inc 2a\nNon-metastatic invasive cancer");
    ExcelUtils.writeCell(headerRow, incPriorHistIdx, "Inc 2b\nNo prior history of contralateral breast cancer");
    ExcelUtils.writeCell(headerRow, incErPosIdx, "Inc 3\nER Positive");
    ExcelUtils.writeCell(headerRow, incSysTherIdx, "Inc 4\nSystemic therapy prior to surgery");
    ExcelUtils.writeCell(headerRow, incAdjTamoxIdx, "Inc 4a\nAdjuvant tamoxifen initiated within 6 months");
    ExcelUtils.writeCell(headerRow, incDurationIdx, "Inc 4b\nTamoxifen duration intended 5 years");
    ExcelUtils.writeCell(headerRow, incTamoxDoseIdx, "Inc 4c\nTamoxifen dose intended 20mg/day");
    ExcelUtils.writeCell(headerRow, incChemoIdx, "Inc 5\nNo adjuvant chemotherapy");
    ExcelUtils.writeCell(headerRow, incHormoneIdx, "Inc 6\nNo additional adjuvant hormonal therapy");
    ExcelUtils.writeCell(headerRow, incDnaCollectionIdx, "Inc 7\nTiming of DNA Collection");
    ExcelUtils.writeCell(headerRow, incFollowupIdx, "Inc 8\nAdequate follow-up");
    ExcelUtils.writeCell(headerRow, incGenoDataAvailIdx, "Inc 9\nCYP2D6 *4 genotype data available for assessment");

    ExcelUtils.writeCell(headerRow, exclude1Idx, "Exclusion 1: time of event unknown");
    ExcelUtils.writeCell(headerRow, exclude2Idx, "Exclusion 2: no followup data");
    ExcelUtils.writeCell(headerRow, exclude3Idx, "Exclusion 3: inconsistent death data");

    ExcelUtils.writeCell(headerRow, includeCrit1Idx, "Criterion 1");
    ExcelUtils.writeCell(headerRow, includeCrit2Idx, "Criterion 2");
    ExcelUtils.writeCell(headerRow, includeCrit3Idx, "Criterion 3");
  }

  private void writeCellDescr(Row descrRow) {
    ExcelUtils.writeCell(descrRow, genoMetabStatusIdx, " Extensive, Intermediate, Poor, or Unknown");
    ExcelUtils.writeCell(descrRow, metabStatusIdx, " Extensive, Intermediate, Poor, or Unknown");
    ExcelUtils.writeCell(descrRow, includeCrit1Idx, "based on Inc 1, 2a, 3, 4b, 4c, 5, 6, 8, 9\nnot otherwise excluded");
    ExcelUtils.writeCell(descrRow, includeCrit2Idx, "based on Inc 2a, 3, 4c, 5, 6, 9\nnot otherwise excluded");
    ExcelUtils.writeCell(descrRow, includeCrit3Idx, "all subjects\nnot otherwise excluded");
  }

  private PoiWorksheetIterator getSampleIterator() {
    return m_sampleIterator;
  }

  private void setSampleIterator(PoiWorksheetIterator sampleIterator) {
    m_sampleIterator = sampleIterator;
  }

  public boolean hasNext() {
    return this.getSampleIterator().hasNext();
  }

  public Subject next() {
    rowIndexPlus();
    return parseSubject(this.getSampleIterator().next());
  }

  public void skipNext() {
    rowIndexPlus();
    this.getSampleIterator().next();
  }

  public void remove() {
    throw new UnsupportedOperationException("org.pharmgkb.ItpcSheet does not support removing Subjects");
  }

  protected Subject parseSubject(List<String> fields) {
    Subject subject = new Subject();

    subject.setSubjectId(fields.get(subjectId));
    subject.setProjectSite(fields.get(projectSiteIdx));
    subject.setAge(fields.get(ageIdx));
    subject.setGender(fields.get(genderIdx));
    subject.setRace(fields.get(raceIdx));
    subject.setMetastatic(fields.get(metastaticIdx));
    subject.setMenoStatus(fields.get(menoStatusIdx));
    subject.setErStatus(fields.get(erStatusIdx));
    subject.setDuration(fields.get(durationIdx));
    subject.setTamoxDose(fields.get(tamoxDoseIdx));
    subject.setTumorSource(fields.get(tumorSourceIdx));
    subject.setBloodSource(fields.get(bloodSourceIdx));
    subject.setPriorHistory(fields.get(priorHistoryIdx));
    subject.setPriorDcis(fields.get(priorDcisIdx));
    subject.setChemotherapy(fields.get(chemoIdx));
    subject.setHormoneTherapy(fields.get(hormoneIdx));
    subject.setSystemicTher(fields.get(systemicTherIdx));
    subject.setFollowup(fields.get(followupIdx));
    subject.setTimeBtwSurgTamox(fields.get(timeBtwSurgTamoxIdx));
    subject.setFirstAdjEndoTher(fields.get(firstAdjEndoTherIdx));
    subject.setTumorDimension(fields.get(tumorDimensionIdx));
    subject.setAdditionalCancer(fields.get(additionalCancerIdx));
    subject.setAddCxIpsilateral(fields.get(addCxIpsilateralIdx));
    subject.setAddCxDistantRecur(fields.get(addCxDistantRecurIdx));
    subject.setAddCxContralateral(fields.get(addCxContralateralIdx));
    subject.setAddCxSecondInvasive(fields.get(addCxSecondInvasiveIdx));
    subject.setAddCxLastEval(fields.get(addCxLastEvalIdx));
    subject.setDaysDiagtoDeath(fields.get(daysDiagToDeathIdx));
    subject.setPatientDied(fields.get(patientDiedIdx));

    if (!StringUtils.isBlank(fields.get(genoSourceIdx1)) && !fields.get(genoSourceIdx1).equals("NA")) {
      subject.setGenoSource(fields.get(genoSourceIdx1));
    }
    else if (!StringUtils.isBlank(fields.get(genoSourceIdx2)) && !fields.get(genoSourceIdx2).equals("NA")) {
      subject.setGenoSource(fields.get(genoSourceIdx2));
    }
    else if (!StringUtils.isBlank(fields.get(genoSourceIdx3)) && !fields.get(genoSourceIdx3).equals("NA")) {
      subject.setGenoSource(fields.get(genoSourceIdx3));
    }

    subject.setHasFluoxetine(translateDrugFieldToValue(fields.get(fluoxetineCol)));
    subject.setHasParoxetine(translateDrugFieldToValue(fields.get(paroxetineCol)));
    subject.setHasQuinidine(translateDrugFieldToValue(fields.get(quinidienCol)));
    subject.setHasBuproprion(translateDrugFieldToValue(fields.get(buproprionCol)));
    subject.setHasDuloxetine(translateDrugFieldToValue(fields.get(duloxetineCol)));
    subject.setHasCimetidine(translateDrugFieldToValue(fields.get(cimetidineCol)));
    subject.setHasSertraline(translateDrugFieldToValue(fields.get(sertralineCol)));
    subject.setHasCitalopram(translateDrugFieldToValue(fields.get(citalopramCol)));

    subject.setRs4986774(new VariantAlleles(fields.get(rs4986774idx)));
    subject.setRs1065852(new VariantAlleles(fields.get(rs1065852idx)));
    subject.setRs3892097(new VariantAlleles(fields.get(rs3892097idx)));
    subject.setRs5030655(new VariantAlleles(fields.get(rs5030655idx)));
    subject.setRs16947(new VariantAlleles(fields.get(rs16947idx)));
    subject.setRs28371706(new VariantAlleles(fields.get(rs28371706idx)));
    subject.setRs28371725(new VariantAlleles(fields.get(rs28371725idx)));
    subject.setDeletion(fields.get(star5idx));

    subject.setGenotypeAmplichip(fields.get(amplichipidx));
    if (fields.size()>otherGenoIdx && !StringUtils.isBlank(fields.get(otherGenoIdx))) {
      subject.setGenotypeAmplichip(fields.get(otherGenoIdx));
    }

    return subject;
  }

  private Value translateDrugFieldToValue(String field) {
    if (ItpcUtils.isBlank(field)) {
      return Value.Unknown;
    }
    else if (field.equals("1")) {
      return Value.Yes;
    }
    else if (field.equals("0")) {
      return Value.No;
    }
    else {
      return Value.Unknown;
    }
  }

  public int getCurrentRowIndex() {
    return m_rowIndex;
  }

  private void rowIndexPlus() {
    m_rowIndex++;
  }

  public Row getCurrentRow() {
    return m_dataSheet.getRow(this.getCurrentRowIndex());
  }

  public void writeSubjectCalculatedColumns(Subject subject) {
    Row row = this.getCurrentRow();
    CellStyle highlight = getHighlightStyle();
    subject.calculateGenotypeLimited();

    ExcelUtils.writeCell(row, allele1finalIdx, subject.getGenotypeFinal().get(0), highlight);
    ExcelUtils.writeCell(row, allele2finalIdx, subject.getGenotypeFinal().get(1), highlight);
    ExcelUtils.writeCell(row, genotypeIdx, subject.getGenotypeFinal().getMetabolizerStatus(), highlight);
    ExcelUtils.writeCell(row, genoMetabStatusIdx, subject.getGenoMetabolizerGroup(), highlight);
    ExcelUtils.writeCell(row, weakIdx, subject.getWeak().toString(), highlight);
    ExcelUtils.writeCell(row, potentIdx, subject.getPotent().toString(), highlight);
    if (subject.getScore()==null) {
      ExcelUtils.writeCell(row, scoreIdx, Value.Unknown.toString(), highlight);
    }
    else {
      ExcelUtils.writeCell(row, scoreIdx, subject.getScore(), highlight);
    }
    ExcelUtils.writeCell(row, metabStatusIdx, subject.getMetabolizerGroup(), highlight);

    ExcelUtils.writeCell(row, incAgeIdx, subject.passInclusion1().toString(), highlight);
    ExcelUtils.writeCell(row, incNonmetaIdx, subject.passInclusion2a().toString(), highlight);
    ExcelUtils.writeCell(row, incPriorHistIdx, subject.passInclusion2b().toString(), highlight);
    ExcelUtils.writeCell(row, incErPosIdx, subject.passInclusion3().toString(), highlight);
    ExcelUtils.writeCell(row, incSysTherIdx, subject.passInclusion4().toString(), highlight);
    ExcelUtils.writeCell(row, incAdjTamoxIdx, subject.passInclusion4a().toString(), highlight);
    ExcelUtils.writeCell(row, incDurationIdx, subject.passInclusion4b().toString(), highlight);
    ExcelUtils.writeCell(row, incTamoxDoseIdx, subject.passInclusion4c().toString(), highlight);
    ExcelUtils.writeCell(row, incChemoIdx, subject.passInclusion5().toString(), highlight);
    ExcelUtils.writeCell(row, incHormoneIdx, subject.passInclusion6().toString(), highlight);
    ExcelUtils.writeCell(row, incDnaCollectionIdx, subject.passInclusion7().toString(), highlight);
    ExcelUtils.writeCell(row, incFollowupIdx, subject.passInclusion8().toString(), highlight);
    ExcelUtils.writeCell(row, incGenoDataAvailIdx, subject.passInclusion9().toString(), highlight);

    ExcelUtils.writeCell(row, exclude1Idx, subject.exclude1().toString(), highlight);
    ExcelUtils.writeCell(row, exclude2Idx, subject.exclude2().toString(), highlight);
    ExcelUtils.writeCell(row, exclude3Idx, subject.exclude3().toString(), highlight);

    ExcelUtils.writeCell(row, includeCrit1Idx, subject.includeCrit1().toString(), highlight);
    ExcelUtils.writeCell(row, includeCrit2Idx, subject.includeCrit2().toString(), highlight);
    ExcelUtils.writeCell(row, includeCrit3Idx, subject.includeCrit3().toString(), highlight);
  }

  public File saveOutput() throws IOException {
    File outputFile = ItpcUtils.getOutputFile(inputFile);
    sf_logger.info("Writing output to: " + outputFile);

    FileOutputStream statsOut = new FileOutputStream(outputFile);
    m_dataSheet.getWorkbook().write(statsOut);
    IOUtils.closeQuietly(statsOut);

    return outputFile;
  }

  public Workbook getWorkbook() {
    return m_dataSheet.getWorkbook();
  }

  public void doHighlighting() {
    if (styleHighlight == null) {
      styleHighlight = getWorkbook().createCellStyle();

      styleHighlight.setFillForegroundColor(HSSFColor.LIGHT_YELLOW.index);
      styleHighlight.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
      styleHighlight.setAlignment(HSSFCellStyle.ALIGN_CENTER);
      styleHighlight.setWrapText(true);
    }
  }

  public CellStyle getHighlightStyle() {
    return styleHighlight;
  }

  /**
   * Styles the given row with the Title Style specified in <code>getTitleStyle</code>. The <code>startIndex</code>
   * parameter specifies which column column to start applying the style on (0 = all columns) inclusively.
   * @param row an Excel Row
   * @param startIndex the index of the column to start applying the style on
   * @param style the CellStyle to apply
   */
  public void styleCells(Row row, int startIndex, CellStyle style) {
    Iterator<Cell> headerCells = row.cellIterator();

    while (headerCells.hasNext()) {
      Cell headerCell=headerCells.next();
      if (headerCell.getColumnIndex()>=startIndex) {
        headerCell.setCellStyle(style);
      }
    }
  }
}
