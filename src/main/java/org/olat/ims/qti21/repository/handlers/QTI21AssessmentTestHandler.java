/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.ims.qti21.repository.handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.Logger;
import org.olat.basesecurity.manager.OrganisationDAO;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.services.license.License;
import org.olat.core.commons.services.license.LicenseService;
import org.olat.core.commons.services.license.model.LicenseImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.layout.MainLayoutController;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Organisation;
import org.olat.core.id.Roles;
import org.olat.core.logging.Tracing;
import org.olat.core.util.FileUtils;
import org.olat.core.util.PathUtils;
import org.olat.core.util.PathUtils.YesMatcher;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.LockResult;
import org.olat.course.assessment.manager.UserCourseInformationsManager;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.FileResource;
import org.olat.fileresource.types.ImsQTI21Resource;
import org.olat.fileresource.types.ResourceEvaluation;
import org.olat.ims.qti21.QTI21DeliveryOptions;
import org.olat.ims.qti21.QTI21Module;
import org.olat.ims.qti21.QTI21Service;
import org.olat.ims.qti21.manager.AssessmentTestSessionDAO;
import org.olat.ims.qti21.model.IdentifierGenerator;
import org.olat.ims.qti21.model.InMemoryOutcomeListener;
import org.olat.ims.qti21.model.QTI21QuestionType;
import org.olat.ims.qti21.model.xml.*;
import org.olat.ims.qti21.model.xml.interactions.MultipleChoiceAssessmentItemBuilder;
import org.olat.ims.qti21.model.xml.interactions.SingleChoiceAssessmentItemBuilder;
import org.olat.ims.qti21.pool.QTI21QPoolServiceProvider;
import org.olat.ims.qti21.ui.AssessmentTestDisplayController;
import org.olat.ims.qti21.ui.QTI21AssessmentDetailsController;
import org.olat.ims.qti21.ui.QTI21OverrideOptions;
import org.olat.ims.qti21.ui.QTI21RuntimeController;
import org.olat.ims.qti21.ui.editor.AssessmentTestComposerController;
import org.olat.modules.assessment.model.QuestionDTO;
import org.olat.modules.assessment.model.SectionDTO;
import org.olat.modules.qpool.QPoolService;
import org.olat.modules.qpool.QuestionItemShort;
import org.olat.modules.qpool.model.QItemList;
import org.olat.repository.RepositoryEntryImportExportLinkEnum;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRuntimeType;
import org.olat.repository.RepositoryEntrySecurity;
import org.olat.repository.RepositoryEntryStatusEnum;
import org.olat.repository.RepositoryService;
import org.olat.repository.handlers.EditionSupport;
import org.olat.repository.handlers.FileHandler;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ed.ph.jqtiplus.node.item.AssessmentItem;
import uk.ac.ed.ph.jqtiplus.node.item.interaction.choice.SimpleChoice;
import uk.ac.ed.ph.jqtiplus.node.test.*;
import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentTest;
import uk.ac.ed.ph.jqtiplus.serialization.QtiSerializer;
import uk.ac.ed.ph.jqtiplus.types.Identifier;

/**
 * 
 * Initial date: 08.12.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class QTI21AssessmentTestHandler extends FileHandler {
	
	private static final Logger log = Tracing.createLoggerFor(QTI21AssessmentTestHandler.class);
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private QTI21Module qtiModule;
	@Autowired
	private QTI21Service qtiService;
	@Autowired
	private QPoolService qpoolService;
	@Autowired
	private LicenseService licenseService;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private QTI21QPoolServiceProvider qpoolServiceProvider;
	
	@Autowired
	private AssessmentTestSessionDAO assessmentTestSessionDao;

    @Autowired
    private OrganisationDAO odao;

	@Override
	public String getSupportedType() {
		return ImsQTI21Resource.TYPE_NAME;
	}

	@Override
	public boolean supportCreate(Identity identity, Roles roles) {
		return true;
	}

	@Override
	public String getCreateLabelI18nKey() {
		return "new.test";
	}

	@Override
	public RepositoryEntry createResource(Identity initialAuthor, String displayname, String description,
			Object createObject, Organisation organisation, Locale locale) {
		ImsQTI21Resource ores = new ImsQTI21Resource();
		
		OLATResource resource = OLATResourceManager.getInstance().findOrPersistResourceable(ores);
		RepositoryEntry re = repositoryService.create(initialAuthor, null, "", displayname, description,
				resource, RepositoryEntryStatusEnum.preparation, RepositoryEntryRuntimeType.embedded, organisation);
		dbInstance.commit();

		File repositoryDir = new File(FileResourceManager.getInstance().getFileResourceRoot(re.getOlatResource()), FileResourceManager.ZIPDIR);
		if(!repositoryDir.exists()) {
			repositoryDir.mkdirs();
		}
		if(createObject instanceof QItemList itemToImport) {
			qpoolServiceProvider.exportToEditorPackage(displayname, repositoryDir,
					itemToImport.getItems(), itemToImport.isGroupByTaxonomyLevel(), locale);
		} else {
			createMinimalAssessmentTest(displayname, repositoryDir, locale);
		}
		return re;
	}
	
	public void createMinimalAssessmentTest(String displayName, File directory, Locale locale) {
        ManifestBuilder manifestBuilder = ManifestBuilder.createAssessmentTestBuilder();

		Translator translator = Util.createPackageTranslator(AssessmentTestComposerController.class, locale);

		//single choice
		File itemFile = new File(directory, IdentifierGenerator.newAsString(QTI21QuestionType.sc.getPrefix()) + ".xml");
		AssessmentItem assessmentItem = AssessmentItemFactory.createSingleChoice(translator.translate("new.sc"), translator.translate("new.answer"));
		QtiSerializer qtiSerializer = qtiService.qtiSerializer();
		manifestBuilder.appendAssessmentItem(itemFile.getName());	
		
		//test
        File testFile = new File(directory, IdentifierGenerator.newAssessmentTestFilename());
		AssessmentTest assessmentTest = AssessmentTestFactory.createAssessmentTest(displayName, translator.translate("new.section"));
		manifestBuilder.appendAssessmentTest(testFile.getName());
        
        // item -> test
        try {
			AssessmentSection section = assessmentTest.getTestParts().get(0).getAssessmentSections().get(0);
			AssessmentTestFactory.appendAssessmentItem(section, itemFile.getName());
		} catch (URISyntaxException e) {
			log.error("", e);
		}
        
        try(FileOutputStream out = new FileOutputStream(itemFile)) {
			qtiSerializer.serializeJqtiObject(assessmentItem, out);	
		} catch(Exception e) {
			log.error("", e);
		}
        
		try(FileOutputStream out = new FileOutputStream(testFile)) {
			qtiSerializer.serializeJqtiObject(assessmentTest, out);	
		} catch(Exception e) {
			log.error("", e);
		}

        manifestBuilder.write(new File(directory, "imsmanifest.xml"));
	}

	@Override
	public boolean supportImport() {
		return true;
	}

	@Override
	public ResourceEvaluation acceptImport(File file, String filename) {
		return ImsQTI21Resource.evaluate(file, filename);
	}

	@Override
	public boolean supportImportUrl() {
		return false;
	}
	
	@Override
	public ResourceEvaluation acceptImport(String url) {
		return ResourceEvaluation.notValid();
	}

	@Override
	public RepositoryEntry importResource(Identity initialAuthor, String initialAuthorAlt, String displayname, String description,
			RepositoryEntryImportExportLinkEnum withLinkedReferences, Organisation organisation, Locale locale, File file, String filename) {
		ImsQTI21Resource ores = new ImsQTI21Resource();
		OLATResource resource = OLATResourceManager.getInstance().createAndPersistOLATResourceInstance(ores);
		File fResourceFileroot = FileResourceManager.getInstance().getFileResourceRoot(resource);
		File zipDir = new File(fResourceFileroot, FileResourceManager.ZIPDIR);
		copyResource(file, filename, zipDir);
		
		File optionsFile = new File(zipDir, QTI21Service.PACKAGE_CONFIG_FILE_NAME);
		if(optionsFile.exists()) {
			try {// move the options to the root directory
				File target = new File(fResourceFileroot, QTI21Service.PACKAGE_CONFIG_FILE_NAME);
				Files.move(optionsFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				log.error("", e);
			}
		} 

		RepositoryEntry re = CoreSpringFactory.getImpl(RepositoryService.class).create(initialAuthor, null, "", displayname, description,
					resource, RepositoryEntryStatusEnum.preparation, RepositoryEntryRuntimeType.embedded, organisation);
		DBFactory.getInstance().commit();
		return re;
	}
	
	private boolean copyResource(File file, String filename, File targetDirectory) {
		try {
			String fallbackEncoding = qtiModule.getImportEncodingFallback();
			Path path = FileResource.getResource(file, filename, fallbackEncoding);
			if(path == null) {
				return false;
			}
			
			Path destDir = targetDirectory.toPath();
			QTI21IMSManifestExplorerVisitor visitor = new QTI21IMSManifestExplorerVisitor();
			Files.walkFileTree(path, visitor);
			Files.walkFileTree(path, new CopyAndConvertVisitor(path, destDir, visitor.getInfos(), new YesMatcher()));
			PathUtils.closeSubsequentFS(path);
			return true;
		} catch (IOException e) {
			log.error("", e);
			return false;
		}
	}
	
	@Override
	public RepositoryEntry importResource(Identity initialAuthor, String initialAuthorAlt, String displayname,
			String description, Organisation organisation, Locale locale, String url) {
		return null;
	}

	@Override
	public MediaResource getAsMediaResource(OLATResourceable res, RepositoryEntryImportExportLinkEnum withLinkedResource) {
		return new QTI21AssessmentTestMediaResource(res);
	}
	
	@Override
	public RepositoryEntry copy(Identity author, RepositoryEntry source, RepositoryEntry target) {
		File sourceRootFile = FileResourceManager.getInstance().getFileResourceRootImpl(source.getOlatResource()).getBasefile();
		File targetRootDir = FileResourceManager.getInstance().getFileResourceRootImpl(target.getOlatResource()).getBasefile();
		File sourceDir = new File(sourceRootFile, FileResourceManager.ZIPDIR);
		File targetDir = new File(targetRootDir, FileResourceManager.ZIPDIR);
		FileUtils.copyDirContentsToDir(sourceDir, targetDir, false, "Copy");
		File sourceOptionsFile = new File(sourceRootFile, QTI21Service.PACKAGE_CONFIG_FILE_NAME);
		if(sourceOptionsFile.exists()) {
			FileUtils.copyFileToDir(sourceOptionsFile, targetRootDir, "Copy QTI 2.1 Options");
		}
		return target;
	}

	@Override
	public boolean supportsDownload() {
		return true;
	}

	@Override
	public EditionSupport supportsEdit(OLATResourceable resource, Identity identity, Roles roles) {
		return EditionSupport.yes;
	}
	
	@Override
	public boolean supportsAssessmentDetails() {
		return true;
	}

	@Override
	public MainLayoutController createLaunchController(RepositoryEntry re, RepositoryEntrySecurity reSecurity,
			UserRequest ureq, WindowControl wControl) {
		return new QTI21RuntimeController(ureq, wControl, re, reSecurity, (uureq, wwControl, toolbarPanel, entry, repoSecurity, mode) -> {
			
			QTI21DeliveryOptions deliveryOptions = qtiService.getDeliveryOptions(entry);
			QTI21OverrideOptions overrideOptions = QTI21OverrideOptions.nothingOverriden();
			if(!deliveryOptions.isAllowAnonym() && uureq.getUserSession().getRoles().isGuestOnly()) {
				Translator translator = Util.createPackageTranslator(QTI21RuntimeController.class, uureq.getLocale());
				Controller contentCtr = MessageUIFactory.createInfoMessage(uureq, wwControl,
						translator.translate("anonym.not.allowed.title"),
						translator.translate("anonym.not.allowed.descr"));
				return new LayoutMain3ColsController(uureq, wwControl, contentCtr);
			}
			boolean authorMode = reSecurity.isEntryAdmin();
			if(authorMode) {
				return new AssessmentTestDisplayController(uureq, wwControl, new InMemoryOutcomeListener(), entry, entry, null,
						deliveryOptions, overrideOptions, BigDecimal.ONE, false, authorMode, true);
			}
			CoreSpringFactory.getImpl(UserCourseInformationsManager.class)
				.updateUserCourseInformations(entry.getOlatResource(), uureq.getIdentity());
			return new AssessmentTestDisplayController(uureq, wwControl, null, entry, entry, null,
					deliveryOptions, overrideOptions, BigDecimal.ONE, false, authorMode, false);
		});
	}

	@Override
	public Controller createEditorController(RepositoryEntry re, UserRequest ureq, WindowControl wControl, TooledStackedPanel toolbar) {
		return new AssessmentTestComposerController(ureq, wControl, toolbar, re);
	}

	@Override
	public Controller createAssessmentDetailsController(RepositoryEntry re, UserRequest ureq, WindowControl wControl,
			TooledStackedPanel toolbar, Identity assessedIdentity) {
		return new QTI21AssessmentDetailsController(ureq, wControl, toolbar, re, assessedIdentity);
	}

	@Override
	public List<License> getElementsLicenses(RepositoryEntry entry) {
		List<License> licenses = new ArrayList<>();
		
		try {
			FileResourceManager frm = FileResourceManager.getInstance();
			File unzippedDirRoot = frm.unzipFileResource(entry.getOlatResource());
			ManifestBuilder manifestBuilder = ManifestBuilder.read(new File(unzippedDirRoot, "imsmanifest.xml"));
			ResolvedAssessmentTest resolvedObject = qtiService.loadAndResolveAssessmentTest(unzippedDirRoot, false, true);
			AssessmentTest assessmentTest = resolvedObject.getRootNodeLookup().extractIfSuccessful();
			List<String> metadataLicenses = new ArrayList<>();
			List<String> metadataIdentifiers = new ArrayList<>();
			for(TestPart part:assessmentTest.getTestParts()) {
				collectElementsLicensesRecursive(part, metadataLicenses, metadataIdentifiers, manifestBuilder);
			}
			
			List<QuestionItemShort> items = qpoolService.loadItemsByIdentifier(metadataIdentifiers);
			if(!items.isEmpty()) {
				licenses.addAll(licenseService.loadLicenses(items));
			}
			if(!metadataLicenses.isEmpty()) {
				for(String metadataLicense:metadataLicenses) {
					License license = new LicenseImpl();
					license.setFreetext(metadataLicense);
					licenses.add(license);
				}
			}
		} catch(Exception e) {
			log.error("", e);
		}

		return licenses;
	}
	
	private void collectElementsLicensesRecursive(AbstractPart part, List<String> licenses, List<String> metadataIdentifiers, ManifestBuilder manifestBuilder) {
		if(part instanceof AssessmentItemRef itemRef) {
			ManifestMetadataBuilder metadata = manifestBuilder.getResourceBuilderByHref(itemRef.getHref().toString());
			if(metadata != null) {
				if(metadata.getLom(false) != null) {
					String license = metadata.getLicense();
					if(StringHelper.containsNonWhitespace(license)) {
						licenses.add(license);
					}
				}
				
				String metadataIdentifier = metadata.getOpenOLATMetadataIdentifier();
				if(!StringHelper.containsNonWhitespace(metadataIdentifier)) {
					metadataIdentifier = metadata.getOpenOLATMetadataMasterIdentifier();
				}
				if(StringHelper.containsNonWhitespace(metadataIdentifier)) {
					metadataIdentifiers.add(metadataIdentifier);
				}
			}
		}

		List<? extends AbstractPart> childParts = part.getChildAbstractParts();
		for(AbstractPart childPart:childParts) {
			collectElementsLicensesRecursive(childPart, licenses, metadataIdentifiers, manifestBuilder);
		}
	}

	@Override
	public LockResult acquireLock(OLATResourceable ores, Identity identity) {
		return null;
	}

	@Override
	public void releaseLock(LockResult lockResult) {
		//
	}

	@Override
	public boolean isLocked(OLATResourceable ores) {
		return false;
	}

	@Override
	public boolean cleanupOnDelete(RepositoryEntry entry, OLATResourceable res) {
		boolean clean = super.cleanupOnDelete(entry, res);
		assessmentTestSessionDao.deleteAllUserTestSessionsByTest(entry);
		return clean;
	}

	@Override
	protected String getDeletedFilePrefix() {
		return null;
	}


    public void migrateTest(String[] to, List<SectionDTO> sections, Identity idn, String org, int id, Map<String, Integer> meta) {
        // intro
        ImsQTI21Resource ores = new ImsQTI21Resource();
        OLATResource resource = OLATResourceManager.getInstance().findOrPersistResourceable(ores);
        RepositoryEntry re = repositoryService.create(idn, null, "",
                to[0], to[1],
                resource, RepositoryEntryStatusEnum.preparation, RepositoryEntryRuntimeType.embedded, odao.loadByLabel(org).get(0));
        QTI21DeliveryOptions options = qtiService.getDeliveryOptions(re);
        options.setShowTitles(false);
        options.setDisplayQuestionProgress(true);
        options.setMaxAttempts(meta.get("liczba_prob"));
        qtiService.setDeliveryOptions(re, options);
        dbInstance.commit();
        File repositoryDir = new File(FileResourceManager.getInstance().getFileResourceRoot(re.getOlatResource()), FileResourceManager.ZIPDIR);
        if(!repositoryDir.exists()) {
            repositoryDir.mkdirs();
        }
        // diff
        String title = sections.get(0).getTitle();
        List<SectionDTO> dbd = sections.stream().flatMap(s -> s.getQsts().stream()).collect(Collectors.groupingBy(q -> q.getDifficulty()))
                .entrySet().stream().map(e -> new SectionDTO(title + " " + e.getKey(), e.getValue())).collect(Collectors.toList());
        // jazda!
        ManifestBuilder manifestBuilder = ManifestBuilder.createAssessmentTestBuilder();
        QtiSerializer qtiSerializer = qtiService.qtiSerializer();
        Map<Integer, Map<File, AssessmentItem>> sectionItems = new HashMap<>();
        for(SectionDTO s : dbd) {
            Map<File, AssessmentItem> items = new HashMap<>();
            for(QuestionDTO q : s.getQsts()) {
                if(q.getType().equals("qnSelectOne") || q.getType().equals("qnSelectBoolean")) {
                    putSingleChoice(q, repositoryDir, manifestBuilder, qtiSerializer, q.getqContent(), items);
                } else if(q.getType().equals("qnSelect")) {
                    putMultiChoice(q, repositoryDir, manifestBuilder, qtiSerializer, q.getqContent(), items);
                } else {
                    log.warn("Unsupported question type putting stub {0}, {1}", q.getType(), q.getqContent());
                    putStub(q, repositoryDir, manifestBuilder, qtiSerializer, q.getqContent(), items);
                }
            }
            sectionItems.put(s.getQsts().get(0).getDifficulty(), items);
        }

        File testFile = new File(repositoryDir, IdentifierGenerator.newAssessmentTestFilename());
        AssessmentTest assessmentTest = AssessmentTestFactory.createAssessmentTest(to[0], to[0]);
        manifestBuilder.appendAssessmentTest(testFile.getName());

        AssessmentTestBuilder tb = new AssessmentTestBuilder(assessmentTest);
        if(meta.get("czas_egzaminu") > 0) {
            tb.setMaximumTimeLimits(Long.valueOf(meta.get("czas_egzaminu")) * 60l);
        }
        BigDecimal tp = new BigDecimal(0);

        Map<Integer, Integer> ptByDiff = getDistribution(
                dbd, meta.get("difficulty_easy"), meta.get("difficulty_middle"), meta.get("difficulty_hard"));

        for (int i = 1; i < dbd.size(); i++) {
            AssessmentTestFactory.appendAssessmentSection(title + i, assessmentTest.getTestParts().get(0));
        }

        Integer total = meta.get("ilosc_pytan") > 0 ? meta.get("ilosc_pytan") : Long.valueOf(
                sectionItems.entrySet().stream().flatMap(e -> e.getValue().entrySet().stream()).count()).intValue();

        for (int i = 0; i < dbd.size(); i++) {
            AssessmentSection as = assessmentTest.getTestParts().get(0).getAssessmentSections().get(i);
            SectionDTO section = dbd.get(i);
            Map<File, AssessmentItem> si = sectionItems.get(section.getDiff());
            BigDecimal mtp = ptByDiff.isEmpty() ?
                    new BigDecimal(1) : ptByDiff.containsKey(section.getDiff()) ?
                    new BigDecimal(ptByDiff.get(section.getDiff())).divide(new BigDecimal(100)) : new BigDecimal(1);
            BigDecimal chose = new BigDecimal(total).multiply(mtp);
            chose = chose.setScale(0, RoundingMode.HALF_UP);
            tp = tp.add(chose.multiply(new BigDecimal(section.getDiff())));

            Selection selection = new Selection(as);
            selection.setSelect(chose.intValue());
            selection.setWithReplacement(true);
            as.setSelection(selection);

            Ordering ordering = new Ordering(as);
            ordering.setShuffle(true);
            as.setOrdering(ordering);
        }

        BigDecimal prog = new BigDecimal(meta.get("prog_zaliczenia")).divide(new BigDecimal(100)).multiply(tp);
        tb.setCutValue(prog.doubleValue());
        tb.build();

        for (int i = 0; i < dbd.size(); i++) {
            AssessmentSection as = assessmentTest.getTestParts().get(0).getAssessmentSections().get(i);
            for(Map.Entry<File, AssessmentItem> en : sectionItems.get(dbd.get(i).getQsts().get(0).getDifficulty()).entrySet()) {
                try {
                    AssessmentTestFactory.appendAssessmentItem(as, en.getKey().getName());
                } catch (URISyntaxException e) {
                    log.error("", e);
                }

                try(FileOutputStream out = new FileOutputStream(en.getKey())) {
                    qtiSerializer.serializeJqtiObject(en.getValue(), out);
                } catch(Exception e) {
                    log.error("", e);
                }
            }
        }

        try(FileOutputStream out = new FileOutputStream(testFile)) {
            qtiSerializer.serializeJqtiObject(assessmentTest, out);
        } catch(Exception e) {
            log.error("", e);
        }

        manifestBuilder.write(new File(repositoryDir, "imsmanifest.xml"));
    }

    private Map<Integer, Integer> getDistribution(List<SectionDTO> scs, int... dist) {
        if(dist.length != 3 || IntStream.of(dist).sum() != 100) return Collections.emptyMap();
        List<SectionDTO> sscs = scs.stream().sorted(Comparator.comparing(SectionDTO::getDiff))
                .collect(Collectors.toList());
        int si = 0;
        Map<Integer, Integer> ret = new HashMap<>();
        for(int i = 0; i < 3; i++) {
            if(dist[i] == 0) continue;
            Integer diff = sscs.get(si).getDiff();
            if(ret.containsKey(diff)) {
                ret.put(diff, dist[i] + ret.get(diff));
            }
            else ret.put(diff, dist[i]);
            if(si < sscs.size() - 1) si++;
        }
        return ret;
    }

    private void putStub(QuestionDTO q, File directory, ManifestBuilder manifestBuilder, QtiSerializer qtiSerializer, String title, Map<File, AssessmentItem> map) {
        File itemFile = new File(directory, IdentifierGenerator.newAsString(QTI21QuestionType.unkown.getPrefix()) + ".xml");
        manifestBuilder.appendAssessmentItem(itemFile.getName());
        AssessmentItem ai = AssessmentItemFactory.createAssessmentItem(QTI21QuestionType.unkown, q.getqContent());
        AssessmentItemFactory.appendDefaultItemBody(ai);
        map.put(itemFile, ai);
    }

    private void putSingleChoice(QuestionDTO q, File directory, ManifestBuilder manifestBuilder, QtiSerializer qtiSerializer, String title, Map<File, AssessmentItem> map) {
        File itemFile = new File(directory, IdentifierGenerator.newAsString(QTI21QuestionType.sc.getPrefix()) + ".xml");
        SingleChoiceAssessmentItemBuilder builder = new SingleChoiceAssessmentItemBuilder(title, "", qtiSerializer);

        builder.setQuestion(q.getqContent());
        builder.setShuffle(true);

        List<SimpleChoice> choiceList = new ArrayList<>();
        int idx = 0;
        for(String ch : q.getaOpts()) {
            SimpleChoice choice = AssessmentItemFactory.createSimpleChoice(builder.getChoiceInteraction(), ch, QTI21QuestionType.mc.getPrefix());
            if(q.getCorrect().contains(idx)) {
                builder.setCorrectAnswer(choice.getIdentifier());
            }
            choiceList.add(choice);
            idx++;
        }
        builder.setSimpleChoices(choiceList);
        builder.setMaxScore(Double.valueOf(q.getDifficulty()));
        builder.build();

        manifestBuilder.appendAssessmentItem(itemFile.getName());
        map.put(itemFile, builder.getAssessmentItem());
    }

    private void putMultiChoice(QuestionDTO q, File directory, ManifestBuilder manifestBuilder, QtiSerializer qtiSerializer, String title, Map<File, AssessmentItem> map) {
        File itemFile = new File(directory, IdentifierGenerator.newAsString(QTI21QuestionType.mc.getPrefix()) + ".xml");
        MultipleChoiceAssessmentItemBuilder builder = new MultipleChoiceAssessmentItemBuilder(title,"", qtiSerializer);
        builder.setQuestion(q.getqContent());
        builder.setShuffle(true);

        List<SimpleChoice> choiceList = new ArrayList<>();
        List<Identifier> correctAnswerList = new ArrayList<>();
        int idx = 0;
        for(String ch : q.getaOpts()) {
            SimpleChoice choice = AssessmentItemFactory.createSimpleChoice(builder.getChoiceInteraction(), ch, QTI21QuestionType.mc.getPrefix());
            if(q.getCorrect().contains(idx)) {
                correctAnswerList.add(choice.getIdentifier());
            }
            choiceList.add(choice);
            idx++;
        }
        builder.setSimpleChoices(choiceList);
        builder.setCorrectAnswers(correctAnswerList);
        builder.setMaxScore(Double.valueOf(q.getDifficulty()));
        builder.build();

        manifestBuilder.appendAssessmentItem(itemFile.getName());
        map.put(itemFile, builder.getAssessmentItem());
    }

}