import requests
from bs4 import BeautifulSoup
from sentence_transformers import SentenceTransformer, util
import os
import time

def smart_sexual_health_scan():
    folder_path = "src/main/resources/article-mds/"
    input_file_path = folder_path + "Conditions A to Z - NHS.html" 
    output_file_path = os.path.join(folder_path, "nhs_links.txt")

    print("1. Loading Model...")
    model = SentenceTransformer('all-mpnet-base-v2')

    # --- 1. TARGET CONCEPT (What we WANT) ---
    target_concept = (
        "Sexually transmitted infections (STIs), HIV, AIDS, Syphilis, Gonorrhoea, Chlamydia, "
        "Genital herpes, Genital warts, Trichomoniasis, Pubic lice, Scabies, Hepatitis B and C. "
        "Reproductive organ health (penis, vagina, vulva, testicles, uterus, ovaries, cervix, prostate). "
        "Sexual dysfunction, erectile dysfunction, premature ejaculation, vaginismus, vulvodynia, "
        "menopause, libido, hormone replacement therapy, testosterone, fertility, and contraception. "
        "Conditions involving genital lumps, discharge, pelvic pain, or soreness."
    )
    
    # --- 2. DISTRACTOR CONCEPT (The "Anti-Target") ---
    # This list pushes down conditions that sound similar but are wrong.
    distractor_concept = (
        # SKIN & NAILS (Confused with Syphilis/Warts/Thrush)
        "Dermatological skin conditions like eczema, psoriasis, acne, hives, cellulitis, impetigo. "
        "Fungal infections of the feet, toes, or nails (athlete's foot, fungal nail). "
        "Childhood viral rashes (chickenpox, measles, hand foot and mouth, slapped cheek). "
        "Warts on hands, fingers or feet (verrucas). Ringworm on the body. "
        
        # RESPIRATORY (Confused with 'Contact' or 'Sex Life' sections)
        "Respiratory illnesses, lung viruses, flu, covid, asthma, bronchitis, "
        "chronic obstructive pulmonary disease (COPD), emphysema, pneumonia, tuberculosis. "
        
        # GENERAL SYSTEMIC (Confused due to 'pain' or 'relationship' mentions)
        "Heart disease, high blood pressure, stroke, diabetes, arthritis, back pain. "
        "Gastrointestinal issues like stomach ache, food poisoning, ibs, diarrhoea, piles. "
        "Dental issues, mouth ulcers, cold sores on the lips. "
        "Orthopedic issues like broken bones, sprains, bunions."
    )

    # --- 3. MINIMAL HARD BLOCK ---
    # Only block things that are strictly irrelevant parts of the body (Feet, Lungs).
    # I removed 'Gynaecomastia', 'Toxic Shock', 'Actinomycosis' so the AI can judge them.
    hard_block_slugs = [
        "fungal-nail-infection", "athletes-foot", "corns-and-calluses", "bunions", 
        "chronic-obstructive-pulmonary-disease-copd", "bronchitis", "emphysema",
        "middle-east-respiratory-syndrome-mers", "flu", "common-cold"
    ]

    print("2. Encoding concepts...")
    target_emb = model.encode(target_concept, convert_to_tensor=True)
    distractor_emb = model.encode(distractor_concept, convert_to_tensor=True)

    print("3. Reading file...")
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
        print(f"File not found at {input_file_path}")
        return

    link_list = sorted(list(all_links))
    print(f"4. Scanning {len(link_list)} pages...")
    
    confirmed_links = []
    headers = {'User-Agent': 'Mozilla/5.0'}

    for i, url in enumerate(link_list):
        slug = url.split("/")[-2].lower()
        
        # Hard Block Check
        if any(bad == slug for bad in hard_block_slugs):
            continue

        if i > 0 and i % 50 == 0: 
            print(f"   ...processed {i}...")

        try:
            # Polite delay to avoid 429 Errors
            time.sleep(0.1) 
            
            response = requests.get(url, headers=headers, timeout=10)
            if response.status_code != 200: 
                print(f"[ERROR] {response.status_code} on {slug}")
                continue

            soup = BeautifulSoup(response.text, 'html.parser')
            # Read 4000 chars to cover "Symptoms" and "Causes"
            page_text = soup.get_text(" ", strip=True)[:4000]
            
            content = f"Condition: {slug}. Text: {page_text}"
            page_emb = model.encode(content, convert_to_tensor=True)

            sex_score = util.cos_sim(target_emb, page_emb)[0].item()
            gen_score = util.cos_sim(distractor_emb, page_emb)[0].item()

            # --- LOGIC ---
            
            # 1. HIGH CONFIDENCE KEEPER (> 0.55)
            # HIV, Syphilis, and Endometriosis will land here.
            if sex_score > 0.55:
                print(f"[KEEP-HIGH] {slug} ({sex_score:.2f})")
                confirmed_links.append(url)
                continue

            # 2. THE BATTLE (> 0.30)
            # Lowered threshold to 0.30 to catch edge cases (like Toxic Shock),
            # BUT they must beat the distractor score significantly.
            if sex_score > 0.30:
                # If Sex Score is clearly higher than Distractor Score
                if sex_score > gen_score:
                    print(f"[KEEP-BATTLE] {slug} (Sex: {sex_score:.2f} > Dist: {gen_score:.2f})")
                    confirmed_links.append(url)
                
                # OPTIONAL: Debug what we dropped to verify
                # else:
                #    print(f"[DROP] {slug} (Distractor: {gen_score:.2f} > Sex: {sex_score:.2f})")

        except Exception as e:
            print(f"[FAIL] {slug}: {e}")
            continue

    # Save
    os.makedirs(folder_path, exist_ok=True)
    with open(output_file_path, "w") as f:
        for link in sorted(confirmed_links):
            f.write(link + "\n")

    print(f"\nSaved {len(confirmed_links)} links.")

if __name__ == "__main__":
    smart_sexual_health_scan()