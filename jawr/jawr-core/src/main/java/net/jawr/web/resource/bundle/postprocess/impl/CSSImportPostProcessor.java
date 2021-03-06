/**
 * Copyright 2009-2016 Ibrahim Chaehoi
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package net.jawr.web.resource.bundle.postprocess.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jawr.web.JawrConstant;
import net.jawr.web.config.JawrConfig;
import net.jawr.web.exception.ResourceNotFoundException;
import net.jawr.web.resource.BinaryResourcesHandler;
import net.jawr.web.resource.bundle.IOUtils;
import net.jawr.web.resource.bundle.css.CssImageUrlRewriter;
import net.jawr.web.resource.bundle.factory.util.PathNormalizer;
import net.jawr.web.resource.bundle.factory.util.RegexUtil;
import net.jawr.web.resource.bundle.generator.GeneratorRegistry;
import net.jawr.web.resource.bundle.generator.ResourceGenerator;
import net.jawr.web.resource.bundle.generator.resolver.SuffixedPathResolver;
import net.jawr.web.resource.bundle.mappings.FilePathMappingUtils;
import net.jawr.web.resource.bundle.postprocess.AbstractChainedResourceBundlePostProcessor;
import net.jawr.web.resource.bundle.postprocess.BundleProcessingStatus;
import net.jawr.web.resource.bundle.postprocess.PostProcessFactoryConstant;
import net.jawr.web.util.StringUtils;

/**
 * This class defines the Post processor which handle the inclusion of the CSS
 * define with @import statement
 * 
 * @author Ibrahim Chaehoi
 * 
 */
public class CSSImportPostProcessor extends AbstractChainedResourceBundlePostProcessor {

	/** The url pattern */
	private static final Pattern IMPORT_PATTERN = Pattern.compile(
			"@import\\s*url\\(\\s*" // 'url(' and any number of whitespaces
					+ "[\"']?(?!(https?:)|(//))([^\"')]*)[\"']?" // any sequence
																	// of
																	// characters,
																	// except an
																	// unescaped
																	// ')'
					+ "\\s*\\)\\s*(\\w+)?\\s*;?", // Any number of whitespaces,
													// then ')'
			Pattern.CASE_INSENSITIVE); // works with 'URL('

	/**
	 * Constructor
	 */
	public CSSImportPostProcessor() {
		super(PostProcessFactoryConstant.CSS_IMPORT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.jawr.web.resource.bundle.postprocess.
	 * AbstractChainedResourceBundlePostProcessor#doPostProcessBundle(net.jawr.
	 * web.resource.bundle.postprocess .BundleProcessingStatus,
	 * java.lang.StringBuffer)
	 */
	@Override
	protected StringBuffer doPostProcessBundle(BundleProcessingStatus status, StringBuffer bundleData)
			throws IOException {

		String data = bundleData.toString();

		// Rewrite each css url path
		Matcher matcher = IMPORT_PATTERN.matcher(data);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {

			String content = getCssPathContent(matcher.group(3), matcher.group(4), status);
			matcher.appendReplacement(sb, RegexUtil.adaptReplacementToMatcher(content));
		}
		matcher.appendTail(sb);
		return sb;

	}

	/**
	 * Retrieve the content of the css to import
	 * 
	 * @param cssPathToImport
	 *            the path of the css to import
	 * @param media
	 *            the media
	 * @param status
	 *            the bundle processing status
	 * @return the content of the css to import
	 * @throws IOException
	 *             if an IOException occurs
	 */
	private String getCssPathContent(String cssPathToImport, String media, BundleProcessingStatus status)
			throws IOException {

		String currentCssPath = status.getLastPathAdded();

		String path = cssPathToImport;

		JawrConfig jawrConfig = status.getJawrConfig();

		if (jawrConfig.getGeneratorRegistry().isPathGenerated(path)) {

			ResourceGenerator generator = jawrConfig.getGeneratorRegistry().getResourceGenerator(path);
			if (generator != null && generator.getResolver() instanceof SuffixedPathResolver) {
				path = PathNormalizer.concatWebPath(currentCssPath, cssPathToImport);
			}
		} else if (!cssPathToImport.startsWith("/")) { // relative URL
			path = PathNormalizer.concatWebPath(currentCssPath, cssPathToImport);
		}

		FilePathMappingUtils.buildFilePathMapping(status.getCurrentBundle(), path, status.getRsReader());
		Reader reader = null;

		try {
			reader = status.getRsReader().getResource(status.getCurrentBundle(), path, true);
		} catch (ResourceNotFoundException e) {
			throw new IOException("Css to import '" + path + "' was not found", e);
		}

		StringWriter content = new StringWriter();
		IOUtils.copy(reader, content, true);

		BinaryResourcesHandler binaryRsHandler = (BinaryResourcesHandler) jawrConfig.getContext()
				.getAttribute(JawrConstant.BINARY_CONTEXT_ATTRIBUTE);
		if (binaryRsHandler != null) {
			jawrConfig = binaryRsHandler.getConfig();
		}
		// Rewrite image URL
		CssImportedUrlRewriter urlRewriter = new CssImportedUrlRewriter(jawrConfig);
		StringBuilder result = new StringBuilder();
		boolean isMediaAttributeSet = StringUtils.isNotEmpty(media);
		if (isMediaAttributeSet) {
			result.append("@media ").append(media).append(" {\n");
		}
		result.append(urlRewriter.rewriteUrl(path, currentCssPath, content.getBuffer().toString()));
		if (isMediaAttributeSet) {
			result.append("\n}\n");
		}
		return result.toString();
	}

	/**
	 * This class rewrite the image URL for the imported CSS.
	 * 
	 * @author Ibrahim Chaehoi
	 */
	private static class CssImportedUrlRewriter extends CssImageUrlRewriter {

		/** The generator registry */
		private final GeneratorRegistry generatorRegistry;

		/**
		 * Constructor
		 * 
		 * @param generatorRegistry
		 *            the generator registry
		 */
		public CssImportedUrlRewriter(JawrConfig jawrConfig) {

			super(jawrConfig);
			this.generatorRegistry = jawrConfig.getGeneratorRegistry();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see net.jawr.web.resource.bundle.css.CssImageUrlRewriter#
		 * getRewrittenImagePath(java.lang.String, java.lang.String,
		 * java.lang.String)
		 */
		@Override
		protected String getRewrittenImagePath(String originalCssPath, String newCssPath, String url)
				throws IOException {

			String currentPath = originalCssPath;

			String imgPath = PathNormalizer.concatWebPath(currentPath, url);
			if (!generatorRegistry.isGeneratedBinaryResource(imgPath)
					&& !generatorRegistry.isHandlingCssImage(originalCssPath)) {
				imgPath = PathNormalizer.getRelativeWebPath(PathNormalizer.getParentPath(newCssPath), imgPath);
			}

			return imgPath;
		}
	}
}
