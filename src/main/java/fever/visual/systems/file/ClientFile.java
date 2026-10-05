package fever.visual.systems.file;

import fever.visual.systems.file.api.FileInfo;
import lombok.Generated;

import java.io.File;

public abstract class ClientFile {
   public final FileInfo infoAnnotation = this.getClass().getAnnotation(FileInfo.class);
   public final File file = new File(FileManager.DIRECTORY, this.infoAnnotation.name() + "." + this.infoAnnotation.fileType());

   public abstract void write();

   public abstract void read();

   @Generated
   public FileInfo getInfoAnnotation() {
      return this.infoAnnotation;
   }

   @Generated
   public File getFile() {
      return this.file;
   }
}
