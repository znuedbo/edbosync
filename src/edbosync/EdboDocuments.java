package edbosync;

import com.google.gson.Gson;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ua.edboservice.ArrayOfDPersonDocumentTypes2;
import ua.edboservice.ArrayOfDPersonDocuments;
import ua.edboservice.ArrayOfDPersonDocumentsSubjects;
import ua.edboservice.DPersonDocumentTypes2;
import ua.edboservice.DPersonDocuments;
import ua.edboservice.DPersonDocumentsSubjects;
import ua.edboservice.EDBOPersonSoap;

/**
 * Класс для синхронизации документов с ЕДБО
 *
 * @author Сергей Чопоров
 */
public class EdboDocuments {

    /**
     * Экземпляр соединения с ЕДБО
     */
    protected EdboPersonConnector edbo = new EdboPersonConnector();
    /**
     * Экземпляр потока SOAP
     */
    protected EDBOPersonSoap soap = edbo.getSoap();
    /**
     * GUID сессии
     */
    protected String sessionGuid = edbo.getSessionGuid();
    /**
     * Текущая дата
     */
    protected String actualDate = edbo.getActualDate();
    /**
     * Идентификатор языка
     */
    protected int languageId = edbo.getLanguageId();

    /**
     * Получить контакты персоны из базы ЕДБО
     *
     * @param personCodeU Код U персоны
     * @return Массив контактов в формате json
     */
    public String load(String personCodeU) {
        Gson json = new Gson();
        ArrayList<PersonDocument> documents = new ArrayList<PersonDocument>();

        ArrayOfDPersonDocuments documentsArray = soap.personDocumentsGet(sessionGuid, actualDate, languageId, personCodeU, 0, 0, edbo.getUniversityKey(), -1);
        if (documentsArray == null) {
            // возникла ошибка при получении данных из ЕДБО
            return edbo.processErrorsJson();
        }
        List<DPersonDocuments> documentsList = documentsArray.getDPersonDocuments();
        for (DPersonDocuments dDocument : documentsList) {
            PersonDocument document = new PersonDocument();
            document.setAttestatValue(dDocument.getAtestatValue());
            document.setDateGet(dDocument.getDocumentDateGet().toGregorianCalendar());
            document.setId_Document(dDocument.getIdPersonDocument());
            document.setId_Type(dDocument.getIdPersonDocumentType());
            document.setIssued(dDocument.getDocumentIssued());
            document.setNumber(dDocument.getDocumentNumbers());
            document.setSeries(dDocument.getDocumentSeries());
            document.setZnoPin(dDocument.getZNOPin());

            if (document.getId_Type() == 4) {
                // документ является сертификатом ЗНО
                ArrayList<DocumentSubject> subjects = new ArrayList<DocumentSubject>();
                ArrayOfDPersonDocumentsSubjects subjectsArray = soap.personDocumentsSubjectsGet(sessionGuid, actualDate, languageId, document.getId_Document(), dDocument.getIdPerson(), document.getId_Type());
                if (subjectsArray == null) {
                    // возникла ошибка при получении данных из ЕДБО
                    return edbo.processErrorsJson();
                }
                List<DPersonDocumentsSubjects> subjectsList = subjectsArray.getDPersonDocumentsSubjects();
                for (DPersonDocumentsSubjects dSubject : subjectsList) {
                    DocumentSubject subject = new DocumentSubject();
                    subject.setId_Subject(dSubject.getIdSubject());
                    subject.setSubjectValue(dSubject.getPersonDocumentSubjectValue());
                    subject.setId_DocumentSubject(dSubject.getIdPersonDocumentSubject());
                    subjects.add(subject);
                }
                document.setSubjects(subjects);
            }

            documents.add(document);
        }
        return json.toJson(documents);
    }

    /**
     * Синхронизировать документы персоны
     *
     * @param personIdMysql Идентификатор персоны в базе данных
     * @return Статус попытки добавить данные в формате json
     * @see SubmitStatus
     */
    public String sync(int personIdMysql) {
        SubmitStatus submitStatus = new SubmitStatus();
        Gson json = new Gson();
        submitStatus.setError(false);
        submitStatus.setBackTransaction(false);
        DataBaseConnector dbc = new DataBaseConnector();
        ArrayList<DocumentSubject> subjects = new ArrayList<DocumentSubject>();
        int edboIdPerson = 0;
        String codeUPerson = "";
        ResultSet person = dbc.executeQuery("SELECT `person`.`edboID`, `person`.`codeU` FROM person WHERE idPerson = " + personIdMysql + ";");
        try {
            if (person.next()) {
                edboIdPerson = person.getInt(1);
                codeUPerson = person.getString(2);
            }
        } catch (SQLException ex) {
            Logger.getLogger(EdboBenefits.class.getName()).log(Level.SEVERE, null, ex);
            submitStatus.setError(true);
            submitStatus.setBackTransaction(false);
            submitStatus.setMessage("Помилка SQL: " + ex.getLocalizedMessage());
            return json.toJson(submitStatus);
        }
        if (edboIdPerson == 0 || codeUPerson.isEmpty()) {
            submitStatus.setError(true);
            submitStatus.setBackTransaction(false);
            submitStatus.setMessage("Неможливо синхронізувати Документи персони, яка не пройшла синхронизацію з ЄДБО.");
            return json.toJson(submitStatus);
        }
        submitStatus.setGuid(codeUPerson);
        submitStatus.setId(edboIdPerson);
        // ЕДБО ----> БД
        // очистим идентифкаторы документов оо образовании для ресинхронизации
//        dbc.executeUpdate("UPDATE `documents`\n"
//                + "SET\n"
//                + "`documents`.`edboID` = null\n"
//                + "WHERE `documents`.`TypeID` in (2, 7,8,9,10,11,12,13,14,15) AND `documents`.`PersonID` = " + personIdMysql + ";");
        ArrayOfDPersonDocuments documentsArray = soap.personDocumentsGet(sessionGuid, actualDate, languageId, codeUPerson, 0, 0, "", -1);
        if (documentsArray == null) {
            // возникла ошибка при получении данных из ЕДБО
            return edbo.processErrorsJson();
        }
        List<DPersonDocuments> documentsList = documentsArray.getDPersonDocuments();
        for (DPersonDocuments dDocument : documentsList) {
            ResultSet documentMySql = dbc.executeQuery("SELECT * FROM `documents` "
                    + "WHERE PersonID = " + personIdMysql + " AND Numbers = \"" + dDocument.getDocumentNumbers() + "\";");
            System.out.println(dDocument.getDocumentNumbers() + "\t" + dDocument.getIdPersonDocument());
            try {
                if (documentMySql.next()) {
                    int docId = documentMySql.getInt("idDocuments");
                    if (dDocument.getIdPersonDocument() != 0) documentMySql.updateInt("edboID", dDocument.getIdPersonDocument());
                    documentMySql.updateRow();
                    System.out.println("updated");
                    if (dDocument.getIdPersonDocumentType() == 4) {
                        // документ является сертификатом ЗНО

                        ArrayOfDPersonDocumentsSubjects subjectsArray = soap.personDocumentsSubjectsGet(sessionGuid, actualDate, languageId, dDocument.getIdPersonDocument(), dDocument.getIdPerson(), dDocument.getIdPersonDocumentType());
                        if (subjectsArray == null) {
                            // возникла ошибка при получении данных из ЕДБО
                            return edbo.processErrorsJson();
                        }
                        List<DPersonDocumentsSubjects> subjectsList = subjectsArray.getDPersonDocumentsSubjects();
                        for (DPersonDocumentsSubjects dSubject : subjectsList) {
                            DocumentSubject subject = new DocumentSubject();
                            subject.setDocumentId(docId);
                            subject.setId_DocumentSubject(dSubject.getIdPersonDocumentSubject());
                            subject.setId_Subject(dSubject.getIdSubject());
                            subject.setSubjectName(dSubject.getSubjectName());
                            subject.setSubjectValue(dSubject.getPersonDocumentSubjectValue());
                            subjects.add(subject);
                            System.out.println(dSubject.getIdSubject() + " " + dSubject.getSubjectName() + " " + dSubject.getPersonDocumentSubjectValue() + " " + dSubject.getIdPersonDocumentSubject());
                        }
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(EdboDocuments.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        String sqlSelectDocuments = "SELECT * FROM documents WHERE PersonID = " + personIdMysql + ";";
        ResultSet document;
        document = dbc.executeQuery(sqlSelectDocuments);
        try {
            while (document.next()) {
                int idDocument = document.getInt("idDocuments");
                int typeId = document.getInt("TypeID");
                String series = document.getString("Series");
                String number = document.getString("Numbers");
                String dateGet = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(document.getDate("DateGet"));
                int znoPin = (typeId == 4) ? document.getInt("ZNOPin") : 0;
                //float attestatval = (typeId == 11 || typeId == 12) ? document.getFloat("AtestatValue") * 10.0f : document.getFloat("AtestatValue");
                String attestatValue = Float.toString(document.getFloat("AtestatValue"));
                String issued = document.getString("Issued");
                int awardTypeId = document.getInt("PersonDocumentsAwardsTypesID");
                int edboId = document.getInt("edboID");
//                int isNotCheckAttestat = document.getInt("isNotCheckAttestat");
//                int isForeinghEntrantDocumet = document.getInt("isForeinghEntrantDocument");
//                int countryId = document.getInt("CountryID");
//                int endYear = Integer.parseInt(document.getString("GraduatedYear"));
//                String specClasifierCode = document.getString("SpecKode");
//                String specClasifierName = document.getString("SpecName");
//                String quailificationName = document.getString("SpecQualification");
// 
                if (document.wasNull() || edboId == 0) {
                    edboId = (typeId == 4)
                            ? soap.personDocumentsZnoAdd(sessionGuid, languageId, edboIdPerson, number, dateGet, znoPin)
                            : soap.personDocumentsAdd(sessionGuid,
                                    languageId,
                                    edboIdPerson,
                                    typeId,
                                    0,
                                    (series != null) ? series : "",
                                    (number != null) ? number : "",
                                    (dateGet != null) ? dateGet : "",
                                    (issued != null) ? issued : "",
                                    "",
                                    znoPin,
                                    attestatValue,
                                    1,
                                    awardTypeId);
//                    soap.personDocumentsAdd2(
//                            sessionGuid,
//                            languageId,
//                            edboIdPerson, 
//                            typeId, 
//                            0, 
//                            (series != null) ? series : "", 
//                            (number != null) ? number : "", 
//                            (dateGet != null) ? dateGet : "",
//                            (issued != null) ? issued : "", 
//                            "", 
//                            znoPin, 
//                            attestatValue, 
//                            1, 
//                            awardTypeId, 
//                            isNotCheckAttestat, 
//                            isForeinghEntrantDocumet, 
//                            documentValueTypeId,
//                            countryId, 
//                            endYear, 
//                            sessionGuid, 
//                            series, 
//                            actualDate, 
//                            idPersonDocumentSourceTypes);
                    if (edboId == 0) {
                        submitStatus.setError(true);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage(submitStatus.getMessage() + number + "  :  " + edbo.processErrors() + "<br />");
//                            System.out.println(number + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription());
                    }
                    else {
                        document.updateInt("edboID", edboId);
                    }
                    document.updateRow();
                    // если работаем с сертификатом, то добавляем перметы в список
                    if (typeId == 4) {
                        ArrayOfDPersonDocumentsSubjects certificatSubjectsArray = soap.personDocumentsSubjectsGet(sessionGuid, actualDate, languageId, edboId, edboIdPerson, typeId);
                        if (certificatSubjectsArray != null) {
                            List<DPersonDocumentsSubjects> documentSubjectsList = certificatSubjectsArray.getDPersonDocumentsSubjects();
                            for (DPersonDocumentsSubjects dSubject : documentSubjectsList) {
                                DocumentSubject subject = new DocumentSubject();
                                subject.setDocumentId(idDocument);
                                subject.setId_DocumentSubject(dSubject.getIdPersonDocumentSubject());
                                subject.setId_Subject(dSubject.getIdSubject());
                                subject.setSubjectName(dSubject.getSubjectName());
                                subject.setSubjectValue(dSubject.getPersonDocumentSubjectValue());
                                subjects.add(subject);
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(EdboDocuments.class.getName()).log(Level.SEVERE, null, ex);
        }
        // Синхронизация списков предметов персоны
        for (DocumentSubject subject : subjects) {
            ResultSet docsubRs = dbc.executeQuery(""
                    + "SELECT idDocumentSubject, edboID, SubjectValue "
                    + "FROM documentsubject "
                    + "WHERE DocumentID = " + subject.getDocumentId() + " AND SubjectID = " + subject.getId_Subject() + ";");
            System.out.println("SELECT idDocumentSubject, edboID, SubjectValue "
                    + "FROM documentsubject "
                    + "WHERE DocumentID = " + subject.getDocumentId() + " AND SubjectID = " + subject.getId_Subject() + ";");

            try {
                if (docsubRs.next()) {
                    // найдена подходящая запись
                    // запись о предмете не была синхронизирована
                    docsubRs.updateDouble("SubjectValue", subject.getSubjectValue());
                    if (subject.getId_DocumentSubject() != 0) docsubRs.updateInt("edboID", subject.getId_DocumentSubject());
                    docsubRs.updateRow();
                    submitStatus.setError(true);
                    submitStatus.setBackTransaction(false);
                    submitStatus.setMessage(submitStatus.getMessage() + "У сертифікаті оновлено предмет: \" " + subject.getSubjectName() + "\" " + "бал: " + Double.toString(subject.getSubjectValue()) + "<br />");
                } else {
                    // вставляем новую запись
                    dbc.executeUpdate("INSERT INTO `documentsubject`\n"
                            + "(\n"
                            + "`DocumentID`,\n"
                            + "`SubjectID`,\n"
                            + "`SubjectValue`,\n"
                            + "`edboID`)\n"
                            + "VALUES\n"
                            + "(\n"
                            + subject.getDocumentId() + ",\n"
                            + subject.getId_Subject() + ",\n"
                            + subject.getSubjectValue() + ",\n"
                            + subject.getId_DocumentSubject() + "\n"
                            + ");");
                    submitStatus.setError(true);
                    submitStatus.setBackTransaction(false);
                    submitStatus.setMessage(submitStatus.getMessage() + "До сертифікату додано предмет: \" " + subject.getSubjectName() + "\" " + "бал: " + Double.toString(subject.getSubjectValue()) + "<br />");
                }
            } catch (SQLException ex) {
                Logger.getLogger(EdboDocuments.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return json.toJson(submitStatus);
    }
    
    public String entrantDocumentValueChange(int idPersonDocumentMySql){
        DataBaseConnector dbc = new DataBaseConnector(); // соедиение с БД
        Gson json = new Gson();
        ResultSet document = dbc.executeQuery("SELECT `AtestatValue`, `edboID` FROM documents WHERE `idDocuments` = " + idPersonDocumentMySql + ";");   
        try {
            if (document.next()) {
                String attestatValue = Float.toString(document.getFloat("AtestatValue"));
                int idEdbo = document.getInt("edboID");
                if (soap.entrantDocumentValueChange(sessionGuid, attestatValue, 1, edbo.getUniversityKey(), idEdbo) == 0) {
                    return edbo.processErrorsJson();
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(EdboDocuments.class.getName()).log(Level.SEVERE, null, ex);
        }
        return json.toJson(true);
    }
    
    public String edit(int idPerson) {
        class DocEditResult{
            public int idDocuments;
            public int idType;
            public int editStatus;
            public String message;
        }
        DataBaseConnector dbc = new DataBaseConnector();
        Gson json = new Gson();
        ArrayList<DocEditResult> result = new ArrayList<DocEditResult>();
        ResultSet personDocs = dbc.executeQuery("SELECT * FROM documents WHERE PersonID = " + idPerson + " and edboID is not null;");
        try {
            while (personDocs.next()) {
                DocEditResult der = new DocEditResult();
                der.idDocuments = personDocs.getInt("idDocuments");
                der.idType = personDocs.getInt("TypeID");
                int idPersondocument = personDocs.getInt("edboID");
                String documentSeries = personDocs.getString("Series");
                String documentNumbers = personDocs.getString("Numbers");
                String documentDateGet = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(personDocs.getDate("DateGet"));
                String documentIssued = personDocs.getString("Issued");
                String description = "";
                int isCheckForPaperCopy = 1;
                int idPersonDocumentsAwardType = personDocs.getInt("PersonDocumentsAwardsTypesID");
                der.editStatus = soap.personDocumentsEdit(sessionGuid, 
                        languageId, 
                        idPersondocument, 
                        0, 
                        documentSeries, 
                        documentNumbers, 
                        documentDateGet, 
                        documentIssued, 
                        description, 
                        isCheckForPaperCopy, 
                        idPersonDocumentsAwardType);
                if (der.editStatus == 0) 
                    der.message = edbo.processErrors();
                else
                    der.message = "";
                result.add(der);
            }
            
            
        } catch (SQLException ex) {
            Logger.getLogger(EdboDocuments.class.getName()).log(Level.SEVERE, null, ex);
        }
        return json.toJson(result);
    }
    
    /**
     * Метод загружает типы документов из ЕДБО
     */
    public void loadDocumentsTypes(){
        DataBaseConnector dbc = new DataBaseConnector();
        ArrayOfDPersonDocumentTypes2 adpdt = soap.personDocumentTypesGet2(sessionGuid, actualDate, languageId);
        if (adpdt == null) {
            // возникла ошибка при получении данных из ЕДБО
            System.err.println(edbo.processErrors());
        }
        List<DPersonDocumentTypes2> documentsTypes = adpdt.getDPersonDocumentTypes2();
        for (DPersonDocumentTypes2 documentsType : documentsTypes) {
            System.out.println(documentsType.getIdPersonDocumentType() + "\t" + documentsType.getPersonDocumentTypeName() + "\t" + documentsType.getIsEntrantDocument());
            String sql = ""
                    + "INSERT INTO `abiturient_2016`.`persondocumenttypes`\n"
                    + "(`idPersonDocumentTypes`,\n"
                    + "`PersonDocumentTypesName`,\n"
                    + "`IsEntrantDocument`)\n"
                    + "VALUES(\n"
                    + documentsType.getIdPersonDocumentType() + ",\n"
                    + "'" + documentsType.getPersonDocumentTypeName() + "',\n"
                    + documentsType.getIsEntrantDocument() + ")"
                    + "on duplicate key update "
                    + "`PersonDocumentTypesName` = " + "'" + documentsType.getPersonDocumentTypeName() + "',\n"
                    + "`IsEntrantDocument` = " + documentsType.getIsEntrantDocument()
                    + ";";
            dbc.executeUpdate(sql);
        }
    }
}
