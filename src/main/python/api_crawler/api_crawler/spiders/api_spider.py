import scrapy
from scrapy.linkextractors import LinkExtractor
from scrapy.spiders import CrawlSpider, Rule
from bs4 import BeautifulSoup
import re

class ApiSpider(CrawlSpider):
    name = 'api_spider'
    allowed_domains = ['bonitasoft.com']  # Replace with the target domain
    start_urls = ['https://www.bonitasoft.com']  # Replace with the starting URL

    keywords = ['api', 'docs', 'swagger', 'openapi']

    rules = (
        Rule(LinkExtractor(allow=(), process_value='process_links'), callback='parse_page', follow=True),
    )

    def process_links(self, value):
        if any(keyword in value for keyword in self.keywords):
            return value
        return None

    def parse_page(self, response):
        # Extract URLs
        for href in response.css('a::attr(href)').getall():
            if href:
                url = response.urljoin(href)
                if any(keyword in url for keyword in self.keywords):
                    yield scrapy.Request(url, callback=self.parse_page, priority=10, meta={'download_timeout': 15})
                else:
                    yield scrapy.Request(url, callback=self.parse_page, priority=1, meta={'download_timeout': 15})

        # Extract content
        page_content = response.text
        soup = BeautifulSoup(page_content, 'lxml')

        # Look for API documentation markers
        if any(keyword in response.url for keyword in self.keywords) or \
           any(keyword in page_content.lower() for keyword in self.keywords):
            yield {
                'url': response.url,
                'title': response.css('title::text').get(),
                'content': page_content,
            }
