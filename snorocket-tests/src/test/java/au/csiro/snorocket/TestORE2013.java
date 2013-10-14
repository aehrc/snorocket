/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket;

import java.io.InputStream;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests from the ORE 2013 workshop reasoner competition.
 * 
 * Issues:
 * 
 * <ol>
 *   <li>The ORE competition uses a 1.6 JVM and our tests have always run with a 1.7 JVM. Several memory improvements 
 *   have been made in version 1.7 so this is likely the cause of most of the OutOfMemoryErrors.</li>
 *   <li>Many failures are because of unsupported features of the EL profile, such as nominals.</li>
 * </ol>
 * 
 * The following is a list of unsupported features that appear in the test ontologies:
 * 
 * <ol>
 *   <li>ClassAssertion</li>
 *   <li>ObjectPropertyAssertion</li>
 *   <li>ObjectPropertyDomain</li>
 *   <li>ObjectPropertyRange</li>
 *   <li>DataPropertyDomain</li>
 *   <li>DataPropertyRange</li>
 *   <li>FunctionalDataProperty</li>
 *   <li>DifferentIndividuals</li>
 *   <li>SubDataProperty</li>
 * </ol>
 *   
 * 
 * @author Alejandro Metke
 *
 */
public class TestORE2013 extends AbstractTest {
    
    /**
     * Taxonomy construction was most likely the reason for the timeout in the ORE evaluation. Tableaux-based reasoners
     * seem to be able to handle it better. Might be interesting to take a closer look at its structure. There might be
     * a bug in the taxonomy calculation or this might be an edge case that really hurts the taxonomy construction
     * algorithm.
     */
    @Ignore
    @Test
    public void test005() {
        evaluate("00757");
    }
    
    /**
     * In the ORE evaluation this test case gives an OutOfMemoryError. Using -Xmx6G also gives an OutOfMemoryError.
     */
    @Ignore
    @Test
    public void test009() {
        evaluate("ncbi-organismal-classification.1132");
    }
    
    /**
     * Elk's result is marked as false. Used FaCT++ to create ground truth.
     * 
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ClassAssertion</li>
     * </ul>
     * 
     */
    @Ignore
    @Test
    public void test014() {
        evaluate("sanou.3090");
    }
    
    /**
     * Elk's result is marked as false. Used FaCT++ to create ground truth.
     * 
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ClassAssertion</li>
     * </ul>
     * 
     */
    @Ignore
    @Test
    public void test015() {
        evaluate("sanou.3091");
    }
    
    /**
     * Result in competition was marked as false but in these results the classification is correct.
     */
    @Test
    public void test001() {
        evaluate("00026");
    }
    
    /**
     * Snorocket does not support ClassAssertion axioms but the classification results are correct.
     * 
     * TODO: implement support for nominals. See paper: Practical Reasoning with Nominals in the EL Family of 
     * Description Logics.
     */
    @Test
    public void test002() {
        evaluate("00659");
    }
    
    /**
     * In the results this is marked as empty for Snorocket and true for ELK, but ELK also produces an empty result.
     */
    @Test
    public void test003() {
        evaluate("6d948fb7-39eb-4731-9d12-8900d6e2aaa4_-s2-s3");
    }
    
    /**
     * In the results this is marked as empty for Snorocket and true for ELK, but ELK also produces an empty result.
     */
    @Test
    public void test004() {
        evaluate("c0cc6a7d-8c78-474b-a7fe-097efd52aabe_-s2-s2");
    }
    
    /**
     * In the ORE evaluation this test case gives an OutOfMemoryError. In this test Snorocket manages to get correct 
     * results using -Xmx6G and a 1.7 JVM.
     */
    @Test
    public void test006() {
        evaluate("2fe93b88-75dc-40d4-b02b-dbb5430aca7f_ntness");
    }
    
    /**
     * In the ORE evaluation this test case gives an OutOfMemoryError. In this test Snorocket manages to get correct 
     * results using -Xmx6G and a 1.7 JVM.
     */
    @Test
    public void test007() {
        evaluate("cell-line-ontology.1245");
    }
    
    /**
     * In the ORE evaluation this test case gives an OutOfMemoryError. In this test Snorocket manages to get correct 
     * results using -Xmx6G and a 1.7 JVM.
     */
    @Test
    public void test008() {
        evaluate("gazetteer.1397");
    }
    
    /**
     * In the ORE evaluation this test case gives an OutOfMemoryError. In this test Snorocket manages to get correct 
     * results using -Xmx6G and a 1.7 JVM.
     */
    @Test
    public void test010() {
        evaluate("snomed_jan11");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ClassAssertion</li>
     *   <li>ObjectPropertyDomain</li>
     *   <li>DifferentIndividuals</li>
     *   <li>ObjectPropertyRange</li>
     * </ul>
     */
    @Test
    public void test011() {
        evaluate("150a71f9-5f85-4065-aefc-561e84b2c621_TITech");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ClassAssertion</li>
     *   <li>DataPropertyRange</li>
     * </ul>
     */
    @Test
    public void test012() {
        evaluate("thesaurus-alternativa.3037");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ClassAssertion</li>
     *   <li>DataPropertyRange</li>
     * </ul>
     */
    @Test
    public void test013() {
        evaluate("thesaurus.3034");
    }
    
    /**
     * Most reasoners create an empty result.
     * 
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>DataPropertyDomain</li>
     *   <li>ObjectPropertyDomain</li>
     *   <li>ObjectPropertyRange</li>
     *   <li>DataPropertyRange</li>
     *   <li>FunctionalDataProperty</li>
     * </ul>
     * 
     */
    @Test
    public void test016() {
        evaluate("proteomics-pipeline-infrastructure-for-cptac.1192");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ObjectPropertyRange</li>
     *   <li>ObjectPropertyDomain</li>
     * </ul>
     * 
     */
    @Test
    public void test017() {
        evaluate("orphanet-ontology-of-rare-diseases.1586");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ObjectPropertyRange</li>
     *   <li>ObjectPropertyDomain</li>
     * </ul>
     * 
     */
    @Test
    public void test018() {
        evaluate("ftc-kb-full");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ClassAssertion</li>
     *   <li>ObjectPropertyAssertion</li>
     * </ul>
     * 
     */
    @Test
    public void test019() {
        evaluate("f2663a02-9a90-4e74-8778-53a42a2bfce3_dBases");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>SubDataProperty</li>
     * </ul>
     * 
     */
    @Test
    public void test020() {
        evaluate("epilepsy.1639");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>DataPropertyRange</li>
     *   <li>SubDataProperty</li>
     * </ul>
     * 
     */
    @Test
    public void test021() {
        evaluate("df892020-3eba-4c93-a2ba-0d0fbc32df2f_cycCore");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ClassAssertion</li>
     *   <li>ObjectPropertyAssertion</li>
     * </ul>
     * 
     */
    @Test
    public void test022() {
        evaluate("cc89d135-6df0-4add-9a5e-a120eed74a7a_tology");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>SubDataProperty</li>
     * </ul>
     * 
     */
    @Test
    public void test023() {
        evaluate("carelex.3008");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ClassAssertion</li>
     *   <li>ObjectPropertyAssertion</li>
     * </ul>
     * 
     */
    @Test
    public void test024() {
        evaluate("baa29363-f93c-4285-827e-0e2380c82efc_cations");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ClassAssertion</li>
     *   <li>ObjectPropertyDomain</li>
     *   <li>ObjectPropertyRange</li>
     * </ul>
     * 
     */
    @Test
    public void test025() {
        evaluate("a1c5ee00-078e-4551-b5b8-1e9c470b1be5_-world");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ClassAssertion</li>
     * </ul>
     * 
     */
    @Test
    public void test026() {
        evaluate("94423a4b-d21f-47cf-ad2a-f8860b21442a_mgi");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ObjectPropertyDomain</li>
     *   <li>ObjectPropertyRange</li>
     * </ul>
     * 
     */
    @Test
    public void test027() {
        evaluate("8bd89c22-d5dc-4367-accf-ec1ce9c2cff8_ocTBox");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ClassAssertion</li>
     *   <li>ObjectPropertyAssertion</li>
     * </ul>
     * 
     */
    @Test
    public void test028() {
        evaluate("7697afec-851e-4ba2-99e3-f9eac12ca7b2_tology");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ClassAssertion</li>
     *   <li>ObjectPropertyDomain</li>
     *   <li>ObjectPropertyRange</li>
     * </ul>
     * 
     */
    @Test
    public void test029() {
        evaluate("750eb57b-0db7-47d7-8f55-121db43201cb_-world");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>DataPropertyDomain</li>
     *   <li>DataPropertyRange</li>
     *   <li>FunctionalDataProperty</li>
     * </ul>
     * 
     */
    @Test
    public void test031() {
        evaluate("6cbda565-f125-400b-93c0-bc32fad26e02_geOntology");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>DataPropertyRange</li>
     *   <li>ObjectPropertyRange</li>
     * </ul>
     * 
     */
    @Test
    public void test032() {
        evaluate("5b94589d-e3a2-4060-9d39-bcf00b5d9a6d_sample");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ClassAssertion</li>
     *   <li>DifferentIndividuals</li>
     *   <li>ObjectPropertyRange</li>
     *   <li>ObjectPropertyDomain</li>
     * </ul>
     * 
     */
    @Test
    public void test033() {
        evaluate("526a1dd9-eeaa-408d-9950-1bfc080fb27c_HU");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>DataPropertyDomain</li>
     *   <li>ObjectPropertyRange</li>
     *   <li>ObjectPropertyDomain</li>
     *   <li>FunctionalDataProperty</li>
     * </ul>
     * 
     * Most reasoners generated empty results, with the exception of treasoner, but a public Protege plugin does not
     * seem to be available so it was impossible to test this.
     */
    @Test
    public void test034() {
        evaluate("4d5252ed-46fe-4bfa-acee-cd3ad7e84f53_1192");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ClassAssertion</li>
     * </ul>
     */
    @Test
    public void test035() {
        evaluate("42ebdf76-4227-4f44-943f-a289db1ffc8f_rences");
    }
    
    /**
     * ELK is marked as TRUE and Snorocket as empty in the ORE2013 results but both are empty.
     */
    @Test
    public void test036() {
        evaluate("413bcbff-7b5c-409b-b716-73ba9611d004_-s2-s1");
    }
    
    /**
     * This ontology uses the following features that are currently unsupported by Snorocket:
     * 
     * <ul>
     *   <li>ObjectPropertyAssertion</li>
     *   <li>ClassAssertion</li>
     * </ul>
     */
    @Test
    public void test037() {
        evaluate("2fa4a317-65b5-4733-ab42-555d54a92895_tology");
    }
    
    @Test
    public void test038() {
        evaluate("00389");
    }

    @Test
    public void test039() {
        evaluate("00392");
    }

    @Test
    public void test040() {
        evaluate("00396");
    }

    @Test
    public void test041() {
        evaluate("00401");
    }

    @Test
    public void test042() {
        evaluate("00417");
    }

    @Test
    public void test043() {
        evaluate("00500");
    }

    @Test
    public void test044() {
        evaluate("00534");
    }

    @Test
    public void test045() {
        evaluate("00538");
    }

    @Test
    public void test046() {
        evaluate("00539");
    }

    @Test
    public void test047() {
        evaluate("00679");
    }

    @Test
    public void test048() {
        evaluate("00685");
    }

    @Test
    public void test049() {
        evaluate("07401b09-06c3-4eb7-9a80-eed25e12d0c9_o");
    }

    @Test
    public void test050() {
        evaluate("0d803acc-6dd0-4ea1-a87e-e514fc52a8a2_pombase");
    }

    @Test
    public void test051() {
        evaluate("166b24fd-a787-45bc-948d-4cee28a955e0_joints");
    }

    @Test
    public void test052() {
        evaluate("1969d9ba-1163-44f2-a860-24f5f3819b6b_nomics");
    }

    @Test
    public void test053() {
        evaluate("21326f40-72b9-4efd-b71f-59018e2d4df1_bo");
    }

    @Test
    public void test054() {
        evaluate("2802e811-637d-4786-8eca-4701b9aad7c4_x-cell");
    }

    @Test
    public void test055() {
        evaluate("2bb2c9e8-88a7-4d79-910d-3bfe54b5e234_onto");
    }

    @Test
    public void test056() {
        evaluate("2d5e702c-baf0-40de-90e5-a544179b717f_natomy");
    }

    @Test
    public void test057() {
        evaluate("35e2a021-cbc7-4e89-9620-bdabe30c4c2e_onto");
    }

    @Test
    public void test058() {
        evaluate("39f6e323-8f42-4384-81c9-cfa861b4d342_imulus");
    }

    @Test
    public void test059() {
        evaluate("3b749695-4da7-4ec3-93f9-29ba00a9180f_mgic");
    }

    @Test
    public void test060() {
        evaluate("45928d2d-611e-4ba6-857b-c363be3a98eb_onto");
    }

    @Test
    public void test061() {
        evaluate("4aba86c0-ac0e-45ed-a92d-81c18e7f91fb_onto");
    }

    @Test
    public void test062() {
        evaluate("4f3dfce1-7973-438c-ae5a-a26354dddb4a__xp_go");
    }

    @Test
    public void test063() {
        evaluate("501679d1-25fc-4e6e-9885-216fc54fdf3d_uality");
    }

    @Test
    public void test064() {
        evaluate("641497bf-49c4-4d4f-b612-538be0456815_animals");
    }

    @Test
    public void test065() {
        evaluate("700d277f-43bb-428a-b39d-f75417dc0e20_quality");
    }

    @Test
    public void test066() {
        evaluate("70409bc3-14a2-4384-a9a6-c579c3fbf729_-mouse");
    }

    @Test
    public void test067() {
        evaluate("729c22ea-b68e-468f-ae48-6fc679930bc8_joints");
    }

    @Test
    public void test068() {
        evaluate("93bbb6b5-2da2-4486-b293-b8e131dab4ee_x-bp-cc");
    }

    @Test
    public void test069() {
        evaluate("948ef783-71ab-4cd3-b547-b0888f726f45_1010");
    }

    @Test
    public void test070() {
        evaluate("9668e823-e908-4326-b2f4-1c88cae5b97d_x-cell");
    }

    @Test
    public void test071() {
        evaluate("981bf49e-5d4f-487d-bab1-c33027311e68_o");
    }

    @Test
    public void test072() {
        evaluate("9c1a3184-ba06-46fd-9759-45635c3f4cdd_-human");
    }

    @Test
    public void test073() {
        evaluate("9fc41ba6-4be7-4452-8adb-13f2a4bb2c6b_-galen");
    }

    @Test
    public void test074() {
        evaluate("a8eba741-3a09-41a1-9ff6-b97c01c92adb_onto");
    }

    @Test
    public void test075() {
        evaluate("a91cb2ca-d48e-464d-a3e4-797735b5dd84_natomy");
    }

    @Test
    public void test076() {
        evaluate("abe6d177-b061-4cdb-b53f-e2dafd659354_adonly");
    }

    @Test
    public void test077() {
        evaluate("ac20d889-f455-4179-8961-f846a281eb41_emical");
    }

    @Test
    public void test078() {
        evaluate("african-traditional-medicine.1099");
    }

    @Test
    public void test079() {
        evaluate("amphibian-gross-anatomy.1090");
    }

    @Test
    public void test080() {
        evaluate("amphibian-taxonomy.1370");
    }

    @Test
    public void test081() {
        evaluate("anatomical-entity-ontology.1568");
    }

    @Test
    public void test082() {
        evaluate("ascomycete-phenotype-ontology.1222");
    }

    @Test
    public void test083() {
        evaluate("b7700fe1-103b-4b32-a21c-f6604a763ba5_t-cell");
    }

    @Test
    public void test084() {
        evaluate("bilateria-anatomy.1114");
    }

    @Test
    public void test085() {
        evaluate("bioinformatics-operations-types-of-data-data-formats-and-topics.1498");
    }

    @Test
    public void test086() {
        evaluate("biological-imaging-methods.1023");
    }

    @Test
    public void test087() {
        evaluate("birnlex.1089");
    }

    @Test
    public void test088() {
        evaluate("breast-tissue-cell-lines.1438");
    }

    @Test
    public void test089() {
        evaluate("brenda-tissue-enzyme-source.1005");
    }

    @Test
    public void test090() {
        evaluate("c3af6ded-8c2f-4142-bd0d-03857687a424_o");
    }

    @Test
    public void test091() {
        evaluate("c-elegans-gross-anatomy.1048");
    }

    @Test
    public void test092() {
        evaluate("c-elegans-phenotype.1067");
    }

    @Test
    public void test093() {
        evaluate("cell-line-ontology.1541");
    }

    @Test
    public void test094() {
        evaluate("cell-type.1006");
    }

    @Test
    public void test095() {
        evaluate("cereal-plant-development.1047");
    }

    @Test
    public void test096() {
        evaluate("cf0d4525-c6c5-4de1-8b69-50a4d37a6cf3_rotein");
    }

    @Test
    public void test097() {
        evaluate("chemical-entities-of-biological-interest.1007");
    }

    @Test
    public void test098() {
        evaluate("clinical-measurement-ontology.1583");
    }

    @Test
    public void test099() {
        evaluate("d0b34f18-93ff-4ef1-ba3a-4c73cc1b25ec__chebi");
    }

    @Test
    public void test100() {
        evaluate("dictyostelium-discoideum-anatomy.1008");
    }

    @Test
    public void test101() {
        evaluate("drosophila-development.1016");
    }

    @Test
    public void test102() {
        evaluate("drosophila-gross-anatomy.1015");
    }

    @Test
    public void test103() {
        evaluate("e5720332-627a-49c7-aa93-833435e6aebc_ternal");
    }

    @Test
    public void test104() {
        evaluate("environment-ontology.1069");
    }

    @Test
    public void test105() {
        evaluate("event-inoh-pathway-ontology-.1011");
    }

    @Test
    public void test106() {
        evaluate("evidence-codes.1012");
    }

    @Test
    public void test107() {
        evaluate("experimental-conditions-ontology.1585");
    }

    @Test
    public void test108() {
        evaluate("exposure-ontology.1575");
    }

    @Test
    public void test109() {
        evaluate("f1ce95ac-1fb1-4171-b8fe-536eeb5a132f_pato");
    }

    @Test
    public void test110() {
        evaluate("f66b9500-799b-4bae-b155-f4ff26ebab19_Bridge");
    }

    @Test
    public void test111() {
        evaluate("f981c4d4-3483-4314-847d-ce1b9f1ee418_onto");
    }

    @Test
    public void test112() {
        evaluate("fda-medical-devices-2010-.1576");
    }

    @Test
    public void test113() {
        evaluate("fission-yeast-phenotype-ontology.1689");
    }

    @Test
    public void test114() {
        evaluate("flybase-controlled-vocabulary.1017");
    }

    @Test
    public void test115() {
        evaluate("fly-taxonomy.1064");
    }

    @Test
    public void test116() {
        evaluate("gene-ontology.1070");
    }

    @Test
    public void test117() {
        evaluate("gene-ontology-extension.1506");
    }

    @Test
    public void test118() {
        evaluate("health_indicators.1581");
    }

    @Test
    public void test119() {
        evaluate("health-level-seven.1343");
    }

    @Test
    public void test120() {
        evaluate("hom-datasource_oshpd.1648");
    }

    @Test
    public void test121() {
        evaluate("hom-datasource_oshpdsc.1667");
    }

    @Test
    public void test122() {
        evaluate("hom-dxprocs_mdcdrg.1642");
    }

    @Test
    public void test123() {
        evaluate("hom-dxvcodes2_oshpd.1654");
    }

    @Test
    public void test124() {
        evaluate("homerun-ontology.1627");
    }

    @Test
    public void test125() {
        evaluate("hom-harvard.1631");
    }

    @Test
    public void test126() {
        evaluate("hom-icd9cm-ecodes.1641");
    }

    @Test
    public void test127() {
        evaluate("hom-icd9_dxandvcodes_oshpd.1647");
    }

    @Test
    public void test128() {
        evaluate("hom-icd9pcs.1625");
    }

    @Test
    public void test129() {
        evaluate("hom-icd9_procs_oshpd.1643");
    }

    @Test
    public void test130() {
        evaluate("hom-mdcdrg.3046");
    }

    @Test
    public void test131() {
        evaluate("hom_mdcs-drgs.1596");
    }

    @Test
    public void test132() {
        evaluate("hom-oshpd.1649");
    }

    @Test
    public void test133() {
        evaluate("hom-oshpd-sc.1668");
    }

    @Test
    public void test134() {
        evaluate("hom-oshpd_usecase.1652");
    }

    @Test
    public void test135() {
        evaluate("hom-procs2_oshpd.1653");
    }

    @Test
    public void test136() {
        evaluate("hugo.1528");
    }

    @Test
    public void test137() {
        evaluate("human-developmental-anatomy-abstract-version.1021");
    }

    @Test
    public void test138() {
        evaluate("human-developmental-anatomy-abstract-version-v2.1517");
    }

    @Test
    public void test139() {
        evaluate("human-developmental-anatomy-timed-version.1022");
    }

    @Test
    public void test140() {
        evaluate("human-disease-ontology.1009");
    }

    @Test
    public void test141() {
        evaluate("human-phenotype-ontology.1125");
    }

    @Test
    public void test142() {
        evaluate("hymenoptera-anatomy-ontology.1362");
    }

    @Test
    public void test143() {
        evaluate("immune-disorder-ontology.3127");
    }

    @Test
    public void test144() {
        evaluate("loggerhead-nesting.1024");
    }

    @Test
    public void test145() {
        evaluate("maize-gross-anatomy.1050");
    }

    @Test
    public void test146() {
        evaluate("malaria-ontology.1311");
    }

    @Test
    public void test147() {
        evaluate("mammalian-phenotype.1025");
    }

    @Test
    public void test148() {
        evaluate("mass-spectrometry.1105");
    }

    @Test
    public void test149() {
        evaluate("measurement-method-ontology.1584");
    }

    @Test
    public void test150() {
        evaluate("medaka-fish-anatomy-and-development.1027");
    }

    @Test
    public void test151() {
        evaluate("mego.1257");
    }

    @Test
    public void test152() {
        evaluate("minimal-anatomical-terminology.1152");
    }

    @Test
    public void test153() {
        evaluate("mixs-controlled-vocabularies.3000");
    }

    @Test
    public void test154() {
        evaluate("molecule-role-inoh-protein-name-family-name-ontology-.1029");
    }

    @Test
    public void test155() {
        evaluate("mosquito-gross-anatomy.1030");
    }

    @Test
    public void test156() {
        evaluate("mosquito-insecticide-resistance.1077");
    }

    @Test
    public void test157() {
        evaluate("mouse-adult-gross-anatomy.1000");
    }

    @Test
    public void test158() {
        evaluate("mouse-gross-anatomy-and-development.1010");
    }

    @Test
    public void test159() {
        evaluate("mouse-pathology.1031");
    }

    @Test
    public void test160() {
        evaluate("multiple-alignment.1026");
    }

    @Test
    public void test161() {
        evaluate("neural-immune-gene-ontology.1539");
    }

    @Test
    public void test162() {
        evaluate("neuro-behavior-ontology.1621");
    }

    @Test
    public void test163() {
        evaluate("ontology-of-glucose-metabolism-disorder.1085");
    }

    @Test
    public void test164() {
        evaluate("pathway-ontology.1035");
    }

    @Test
    public void test165() {
        evaluate("pediatric-terminology.1640");
    }

    @Test
    public void test166() {
        evaluate("phenotypic-quality.1107");
    }

    @Test
    public void test167() {
        evaluate("phenx-terms.3078");
    }

    @Test
    public void test168() {
        evaluate("physico-chemical-methods-and-properties.1014");
    }

    @Test
    public void test169() {
        evaluate("physico-chemical-process.1043");
    }

    @Test
    public void test170() {
        evaluate("plant-anatomy.1108");
    }

    @Test
    public void test171() {
        evaluate("plant-environmental-conditions.1036");
    }

    @Test
    public void test172() {
        evaluate("plant-ontology.1587");
    }

    @Test
    public void test173() {
        evaluate("plant-structure-development-stage.1038");
    }

    @Test
    public void test174() {
        evaluate("plant-trait-ontology.1037");
    }

    @Test
    public void test175() {
        evaluate("protein-modification.1041");
    }

    @Test
    public void test176() {
        evaluate("protein-protein-interaction.1040");
    }

    @Test
    public void test177() {
        evaluate("rat-strain-ontology.1150");
    }

    @Test
    public void test178() {
        evaluate("robert-hoehndorf-s-version-of-mesh.3019");
    }

    @Test
    public void test179() {
        evaluate("sample-processing-and-separation-techniques.1044");
    }

    @Test
    public void test180() {
        evaluate("solanaceae-phenotype-ontology.3029");
    }

    @Test
    public void test181() {
        evaluate("soyontology.3028");
    }

    @Test
    public void test182() {
        evaluate("spider-ontology.1091");
    }

    @Test
    public void test183() {
        evaluate("symptom-ontology.1224");
    }

    @Test
    public void test184() {
        evaluate("systems-biology.1046");
    }

    @Test
    public void test185() {
        evaluate("teleost-taxonomy.1081");
    }

    @Test
    public void test186() {
        evaluate("tick-gross-anatomy.1065");
    }

    @Test
    public void test187() {
        evaluate("units-of-measurement.1112");
    }

    @Test
    public void test188() {
        evaluate("vertebrate-homologous-organ-groups.1574");
    }

    @Test
    public void test189() {
        evaluate("vertebrate-skeletal-anatomy-ontology.1555");
    }

    @Test
    public void test190() {
        evaluate("vertebrate-trait-ontology.1659");
    }

    @Test
    public void test191() {
        evaluate("wheat-trait.1545");
    }

    @Test
    public void test192() {
        evaluate("xeml-environment-ontology.3176");
    }

    @Test
    public void test193() {
        evaluate("xenopus-anatomy-and-development.1095");
    }

    @Test
    public void test194() {
        evaluate("yeast-phenotypes.1115");
    }

    @Test
    public void test195() {
        evaluate("zebrafish-anatomy-and-development.1051");
    }
    
    private void evaluate(String name) {
        System.out.println("Testing " + name);
        InputStream stated = this.getClass().getResourceAsStream("/" + name + ".owl");
        InputStream inferred = this.getClass().getResourceAsStream("/" + name + "_inferred.owl");
        testOWLOntology(stated, inferred, false);
    }
    
}
