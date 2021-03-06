package com.sap.codeinsights;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import org.eclipse.jgit.revwalk.RevCommit;

public class DocumentationProcessor extends Processor {

	public static final String TYPE = "documentationprocessor";

	private List<DocumentationCoder> documentationCoders;
	private static final String[] FILE_FILTER = new String[]{"java"};

	public DocumentationProcessor(CodeRequest request, Updatable updater) {
		super(request, updater);
	}

	public List<DocumentationCoder> getCoders() {
		List<DocumentationCoder> documentationCoders = new ArrayList<DocumentationCoder>();
		try {
			Iterable<RevCommit> commits = super.repo.log().all().call();
			for (RevCommit rc : commits) {
				DocumentationCoder documentationCoder = new DocumentationCoder(rc.getAuthorIdent());
				if (!documentationCoders.contains(documentationCoder)) {
					documentationCoders.add(documentationCoder);
				}
			}
			return documentationCoders;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void processFile(File file) {
		updater.pushUpdate(new Update(0, "Processing file: " + file.getName()));
		try {
			new VoidVisitorAdapter<Object>() {
                @Override
                public void visit(MethodDeclaration n, Object args) {
                    if (n.getComment().isPresent()) {
                        hasComments(n, file);
                    } else {
                        noComments(n, file);
                    }
                }
            }.visit(JavaParser.parse(file), null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void getResult(Resultable result) {
		updater.pushUpdate(new Update(0, "Started Request"));

		try {
			updater.pushUpdate(new Update(0, "Cloning Repository"));
			super.cloneRepo();
			this.documentationCoders = getCoders();

			File repoDir = repo.getRepository().getDirectory().getParentFile();
			List<File> files = (List<File>) FileUtils.listFiles(repoDir, FILE_FILTER, true);

			for (File file : files) {
				processFile(file);
			}

			updater.pushUpdate(new Update(0, "Forming result"));
			result.setResult(documentationCoders);
			updater.pushUpdate(new Update(1, "Done."));

		} catch (Exception e) {
			// TODO handle on your own.
			e.printStackTrace();
		}
	}

	private void hasComments(MethodDeclaration n, File file) {
		Comment comment = n.getComment().get();
		ArrayList<DocumentationCoder> commenters = new ArrayList<DocumentationCoder>();
		ArrayList<DocumentationCoder> programmers = new ArrayList<DocumentationCoder>();
		ArrayList<DocumentationCoder> allContributors = new ArrayList<DocumentationCoder>();

		try {
			for (int i = comment.getBegin().get().line; i <= comment.getEnd().get().line; i++) {
				DocumentationCoder commenter = new DocumentationCoder(super.repo.blame().setFilePath(path(file)).call().getSourceAuthor(i - 1));
				if (!commenters.contains(commenter)) {
					commenters.add(commenter);
				}
			}

			for (int i = n.getBegin().get().line; i <= n.getEnd().get().line; i++) {
				DocumentationCoder programmer = new DocumentationCoder(super.repo.blame().setFilePath(path(file)).call().getSourceAuthor(i - 1));
				if (!programmers.contains(programmer)) {
					programmers.add(programmer);
				}
			}
		} catch (GitAPIException e) {
			e.printStackTrace();
		}

		allContributors.addAll(programmers);
		for (DocumentationCoder c : commenters) {
			if (!allContributors.contains(c)) {
				allContributors.add(c);
			}
		}

		for (DocumentationCoder contributor : allContributors) {
			documentationCoders.get(documentationCoders.indexOf(contributor)).methodsContributed++;
		}

		for (DocumentationCoder commenter : commenters) {
			documentationCoders.get(documentationCoders.indexOf(commenter)).documentationContributed++;
		}

		for (DocumentationCoder programmer : programmers) {
			documentationCoders.get(documentationCoders.indexOf(programmer)).documentedMethods++;
		}
	}

	// TODO refactor to hashset of some sort
	private void noComments(MethodDeclaration n, File file) {
		ArrayList<DocumentationCoder> programmers = new ArrayList<DocumentationCoder>();

		try {
			for (int i = n.getBegin().get().line; i <= n.getEnd().get().line; i++) {
				DocumentationCoder programmer = new DocumentationCoder(super.repo
					.blame()
					.setFilePath(path(file))
					.call()
					.getSourceAuthor(i - 1)
				);

				if (!programmers.contains(programmer)) {
					programmers.add(programmer);
				}
			}
		} catch (GitAPIException e) {
			e.printStackTrace();
		}

		for (DocumentationCoder programmer : programmers) {
			documentationCoders.get(documentationCoders.indexOf(programmer)).methodsContributed++;
			documentationCoders.get(documentationCoders.indexOf(programmer)).undocumentedMethods++;
		}
	}

	private String path(File file) {
		return file.getAbsolutePath().replace(super.repo.getRepository().getDirectory().getParentFile().getAbsolutePath() + "/", "");
	}

	@Override
	public String toString() {
		return getType();
	}

	@Override
	public String getType() {
		return TYPE;
	}
}
