/*
 * WekaDeeplearning4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WekaDeeplearning4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WekaDeeplearning4j.  If not, see <https://www.gnu.org/licenses/>.
 *
 * DatasetLoader.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package robbeloth.research;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.dl4j.iterators.instance.ImageInstanceIterator;

/**
 * Utility class for loading datasets in JUnit tests
 *
 * @author Steven Lang
 */
public class DatasetLoader {

  /**
   * Number of classes in the iris dataset
   */
  public static final int NUM_CLASSES_IRIS = 3;

  /**
   * Number of classes in the mnist dataset
   */
  public static final int NUM_CLASSES_MNIST = 10;

  /**
   * Number of classes in the diabetes dataset
   */
  public static final int NUM_CLASSES_DIABETES = 1;

  /**
   * Number of classes in the plant-seedlings-small dataset
   */
  public static final int NUM_CLASSES_PLANT_SEEDLINGS = 5;

  /**
   * Number of instances in the iris dataset
   */
  public static final int NUM_INSTANCES_IRIS = 150;

  /**
   * Number of instances in the mnist dataset
   */
  public static final int NUM_INSTANCES_MNIST = 420;

  /**
   * Number of instances in the diabetes dataset
   */
  public static final int NUM_INSTANCES_DIABETES = 43;

  /**
   * Number of instances in the small plant-seedlings dataset
   */
  public static final int NUM_INSTANCES_PLANT_SEEDLINGS = 25;

  /**
   * Filepath to the plant-seedlings dataset
   */
  public static final String FILE_PATH_PLANT_SEEDLINGS = "src/test/resources/nominal/plant-seedlings-small";

  /**
   * Number of attributes for a 'meta' arff - path to the image file, and class of the image
   */
  public static final int NUM_ATTRIBUTES_IMAGE_META = 2;


  /**
   * Load the mnist minimal dataset with an ImageInstanceIterator
   *
   * @return ImageInstanceIterator
   */
  public static ImageInstanceIterator loadMiniMnistImageIterator() {
    return loadMnistImageIterator("src/test/resources/nominal/mnist-minimal");
  }

  public static ImageInstanceIterator loadMiniDelaunayImageIterator() {
	  return loadMnistImageIterator("imgDump/");
  }
  
  /**
   * Load the mnist minimal dataset with an ImageInstanceIterator
   *
   * @return ImageInstanceIterator
   */
  public static ImageInstanceIterator loadMnistImageIterator(String path) {
    ImageInstanceIterator imgIter = new ImageInstanceIterator();
    imgIter.setImagesLocation(new File(path));
    final int height = 28;
    final int width = 28;
    final int channels = 1;
    imgIter.setHeight(height);
    imgIter.setWidth(width);
    imgIter.setNumChannels(channels);
    return imgIter;
  }

  /**
   * Load the iris arff file
   *
   * @return Iris data as Instances
   * @throws Exception IO error.
   */
  public static Instances loadIris() throws Exception {
    Instances data = new Instances(new FileReader("src/test/resources/nominal/iris.arff"));
    data.setClassIndex(data.numAttributes() - 1);
    return data;
  }

  /**
   * Load the iris arff file with missing values
   *
   * @return Iris data as Instances
   * @throws Exception IO error.
   */
  public static Instances loadIrisMissingValues() throws Exception {
    Instances data =
        new Instances(new FileReader("src/test/resources/nominal/iris-missing-values.arff"));
    data.setClassIndex(data.numAttributes() - 1);
    return data;
  }

  /**
   * Load the diabetes arff file
   *
   * @return Diabetes data as Instances
   * @throws Exception IO error.
   */
  public static Instances loadDiabetes() throws Exception {
    Instances data =
        new Instances(new FileReader("src/test/resources/numeric/diabetes_numeric.arff"));
    data.setClassIndex(data.numAttributes() - 1);
    return data;
  }

  /**
   * Load the mnist minimal meta arff file
   *
   * @return Mnist minimal meta data as Instances
   * @throws Exception IO error.
   */
  public static Instances loadMiniMnistMeta() throws Exception {
    return loadArff("src/test/resources/nominal/mnist.meta.minimal.arff");
  }

  public static Instances loadDelaunayMeta() throws Exception {
	  return loadArff("output/IMG_Database.arff");
  }
  
  /**
   * Load the glass arff file
   *
   * @return Glass data as Instances
   * @throws Exception IO error.
   */
  public static Instances loadGlass() throws Exception {
    return loadArff("src/test/resources/nominal/glass.arff");
  }

  /**
   * Load the ReutersCorn train arff file (minimal)
   *
   * @return ReutersCorn train instances
   * @throws Exception IO error.
   */
  public static Instances loadReutersMinimal() throws Exception {
    return loadArff("src/test/resources/nominal/ReutersCorn-train-minimal.arff");
  }

  /**
   * Load the ReutersCorn train arff file (full)
   *
   * @return ReutersCorn train instances
   * @throws Exception IO error.
   */
  public static Instances loadReutersFull() throws Exception {
    return loadArff("src/test/resources/nominal/ReutersCorn-train-full.arff");
  }

  /**
   * GZIP unzip from: https://www.mkyong.com/java/how-to-decompress-file-from-gzip-file/
   */
  public static void gunzip(String inputPath, String outputPath) {

    byte[] buffer = new byte[1024];

    try {

      GZIPInputStream gzis =
          new GZIPInputStream(new FileInputStream(inputPath));

      FileOutputStream out =
          new FileOutputStream(outputPath);

      int len;
      while ((len = gzis.read(buffer)) > 0) {
        out.write(buffer, 0, len);
      }

      gzis.close();
      out.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Load the mnist minimal arff file
   *
   * @return Mnist minimal arff data as Instances
   * @throws Exception IO error.
   */
  public static Instances loadMiniMnistArff() throws Exception {
    return loadArff("src/test/resources/nominal/mnist_784_train_minimal.arff");
  }

  /**
   * Load the wine_date arff file
   *
   * @return Wine date data
   * @throws Exception IO error.
   */
  public static Instances loadWineDate() throws Exception {
    return loadArff("src/test/resources/date/wine_date.arff");
  }

  /**
   * Load the fishcatch arff file
   *
   * @return Fish catch data
   * @throws Exception IO error.
   */
  public static Instances loadFishCatch() throws Exception {
    return loadArff("src/test/resources/numeric/fishcatch.arff");
  }

  /**
   * Load the anger arff file
   *
   * @return Anger data
   * @throws Exception IO error.
   */
  public static Instances loadAnger() throws Exception {
    return loadArff("src/test/resources/numeric/anger.arff");
  }

  /**
   * Load an arbitrary arff file
   *
   * @return Instances
   * @throws Exception IO error.
   */
  public static Instances loadArff(String path) throws Exception {
    Instances data = new Instances(new FileReader(path));
    data.setClassIndex(data.numAttributes() - 1);
    return data;
  }

  /**
   * Load the mnist minimal meta arff file
   *
   * @return Mnist minimal meta data as Instances
   * @throws Exception IO error.
   */
  public static Instances loadCSV(String path) throws Exception {
    CSVLoader csv = new CSVLoader();
    csv.setSource(new File(path));
    Instances data = csv.getDataSet();
    data.setClassIndex(data.numAttributes() - 1);
    return data;
  }

  public static File loadAngerFilesDir() {
    return new File("src/test/resources/numeric/anger-texts");
  }

  public static Instances loadAngerMeta() throws Exception {
    return DatasetLoader.loadArff("src/test/resources/numeric/anger.meta.arff");
  }

  public static Instances loadAngerMetaClassification() throws Exception {
    final Instances data = DatasetLoader
        .loadArff("src/test/resources/numeric/anger.meta.arff");
    ArrayList<Attribute> atts = new ArrayList<>();
    atts.add(data.attribute(0));
    Attribute cls = new Attribute("cls", Arrays.asList("0", "1"));
    atts.add(cls);
    Instances dataDiscretized = new Instances("anger-classification", atts, data.numInstances());
    dataDiscretized.setClassIndex(1);
    for (Instance datum : data) {
      Instance cpy = (Instance) datum.copy();
      cpy.setDataset(dataDiscretized);
      cpy.setValue(0, datum.stringValue(0));
      cpy.setValue(1, datum.classValue() > 0.5 ? "1" : "0");
      dataDiscretized.add(cpy);
    }
    return dataDiscretized;
  }

  public static Instances loadRelationalNumericClass() throws Exception {
    return DatasetLoader
        .loadArff("src/test/resources/relational/relational-data-numeric-class.arff");
  }

  public static Instances loadRelationalNominalClass() throws Exception {
    return DatasetLoader
        .loadArff("src/test/resources/relational/relational-data-nominal-class.arff");
  }

}

