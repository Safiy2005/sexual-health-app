import requests
from bs4 import BeautifulSoup
from sentence_transformers import SentenceTransformer, util
import os

def pure_ai_scan():
    folder_path = "src/main/resources/article-mds/"
    input_file_path = folder_path+"Conditions A to Z - NHS.html" 
  
    output_file_path = os.path.join(folder_path, "nhs_links.txt")

    print("1. Loading High-Accuracy Model (all-mpnet-base-v2)...")
    model = SentenceTransformer('all-mpnet-base-v2')

    # --- THE CONCEPTS ---
    
    # CONCEPT A: WHAT WE WANT (Sexual Health)
    # We use strong keywords for genitals and transmission.
    target_concept = (
        "Comprehensive sexual and reproductive health, including sexually transmitted infections (STIs), "
        "genital and reproductive organ conditions (penis, vagina, vulva, testicles, uterus, ovaries, cervix), "
        "sexual dysfunction, libido, and arousal issues. It covers contraception, birth control, "
        "fertility, pregnancy prevention, and hormone-related sexual health (menopause, testosterone). "
        "Also includes clinical symptoms like genital discharge, lumps, sores, pelvic pain, "
        "and cancers of the reproductive system (cervical, penile, testicular, ovarian)."
    )
    
    # CONCEPT B: WHAT WE WANT TO FILTER (The "Distractors")
    # This concept attracts the false positives so they score higher here than on Concept A.
    distractor_concept = (
        "Childhood rashes like chickenpox and measles, fungal nail infections on feet, "
        "general skin infections on hands, face or legs (cellulitis, impetigo), "
        "respiratory flu, lung viruses, asthma, coughing, "
        "digestive issues like stomach ache, bowel problems, food poisoning, "
        "kidney stones, bladder cancer, general urinary issues not related to sex, "
        "routine pregnancy, labour and childbirth, bedsores, gangrene, and mouth ulcers."
    )

    # Encode both
    target_emb = model.encode(target_concept, convert_to_tensor=True)
    distractor_emb = model.encode(distractor_concept, convert_to_tensor=True)

    print("2. Reading file...")
    all_links = set()
    try:
        with open(input_file_path, "r", encoding="utf-8") as f:
            soup = BeautifulSoup(f.read(), 'html.parser')
        for a in soup.find_all('a', href=True):
            href = a['href']
            if "/conditions/" in href and len(href) > 12:
                full_url = f"https://www.nhs.uk{href}" if not href.startswith("http") else href
                all_links.add(full_url)
    except FileNotFoundError:
        print("File not found.")
        return

    link_list = sorted(list(all_links))
    print(f"3. Scanning {len(link_list)} pages with Pure AI Logic...")
    
    confirmed_links = []
    headers = {'User-Agent': 'Mozilla/5.0'}

    for i, url in enumerate(link_list):
        slug = url.split("/")[-2].lower()
        
        # Progress log
        if i > 0 and i % 25 == 0: print(f"   ...processed {i}...")

        try:
            # Fetch
            response = requests.get(url, headers=headers, timeout=5)
            if response.status_code != 200: continue

            # Read text: Intro + Symptoms (first 3000 chars)
            soup = BeautifulSoup(response.text, 'html.parser')
            page_text = soup.get_text(" ", strip=True)[:3000]
            
            # Prepare content for AI
            content_to_grade = f"Condition: {slug}. Text: {page_text}"
            page_embedding = model.encode(content_to_grade, convert_to_tensor=True)

            # SCORING
            sex_score = util.cos_sim(target_emb, page_embedding)[0].item()
            general_score = util.cos_sim(distractor_emb, page_embedding)[0].item()

            # --- PURE LOGIC (No Hard Blocks) ---
            
            # 1. Base Relevance: Must be decently related to sexual health
            if sex_score > 0.35:
                
                # 2. The "Battle": Sexual score must be HIGHER than Distractor score
                if sex_score > general_score:
                    print(f"[MATCH] {slug} (Sex: {sex_score:.2f} > Distractor: {general_score:.2f})")
                    confirmed_links.append(url)
                else:
                    # It was related, but looked MORE like a general skin/childhood issue
                    # print(f"[DROP ] {slug} (More like distractor: {general_score:.2f})")
                    pass

        except Exception:
            continue

    # --- SAVE ---
    os.makedirs(folder_path, exist_ok=True)
    with open(output_file_path, "w") as f:
        for link in sorted(confirmed_links):
            f.write(link + "\n")

    print(f"\nSaved {len(confirmed_links)} links to {output_file_path}")

if __name__ == "__main__":
    pure_ai_scan()